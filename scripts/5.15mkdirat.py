#!/usr/bin/env python3

import os
import re
import sys
import shutil
from pathlib import Path

def backup_file(file_path):
    backup_path = file_path + ".backup"
    shutil.copy2(file_path, backup_path)
    print(f"已创建备份: {backup_path}")
    return backup_path

def find_do_mkdirat_function(content):
    pattern = r'int\s+do_mkdirat\s*\([^)]+\)\s*\{'
    match = re.search(pattern, content)
    if not match:
        raise ValueError("未找到do_mkdirat函数")
    
    start_pos = match.start()
    print(f"找到do_mkdirat函数，位置: {start_pos}")
    return start_pos

def find_function_end(content, start_pos):
    brace_count = 0
    in_function = False
    
    for i in range(start_pos, len(content)):
        if content[i] == '{':
            brace_count += 1
            in_function = True
        elif content[i] == '}':
            brace_count -= 1
            if in_function and brace_count == 0:
                return i
    
    raise ValueError("未找到函数结束位置")

def find_insertion_points(content, function_start):
    func_content = content[function_start:]
    
    lookup_flags_pattern = r'unsigned int lookup_flags\s*=\s*LOOKUP_DIRECTORY;'
    lookup_match = re.search(lookup_flags_pattern, func_content)
    
    if not lookup_match:
        raise ValueError("未找到LOOKUP_DIRECTORY声明")
    
    var_decl_pos = function_start + lookup_match.end()
    
    is_err_pattern = r'if\s*\(\s*IS_ERR\s*\(\s*dentry\s*\)\s*\)\s*goto\s+out_putname;'
    is_err_match = re.search(is_err_pattern, func_content)
    
    if not is_err_match:
        is_err_pattern2 = r'if\s*\(\s*IS_ERR\s*\(\s*dentry\s*\)\s*\)\s*return\s+PTR_ERR\s*\(\s*dentry\s*\);'
        is_err_match = re.search(is_err_pattern2, func_content)
        
        if not is_err_match:
            raise ValueError("未找到IS_ERR(dentry)检查")
    
    security_check_pos = function_start + is_err_match.end()
    
    return var_decl_pos, security_check_pos

def insert_fmac_check(file_path):
    print(f"处理文件: {file_path}")
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    backup_path = backup_file(file_path)
    
    try:
        vfs_mkdir_end_pattern = r'EXPORT_SYMBOL_NS\s*\(\s*vfs_mkdir\s*,\s*ANDROID_GKI_VFS_EXPORT_ONLY\s*\)\s*;'
        vfs_mkdir_match = re.search(vfs_mkdir_end_pattern, content)
        
        if vfs_mkdir_match:
            declaration_pos = vfs_mkdir_match.end()
            declaration = '\n\nint fmac_check_mkdirat(const char __user *pathname);'
            
            if 'fmac_check_mkdirat' not in content:
                content = content[:declaration_pos] + declaration + content[declaration_pos:]
                print("已添加fmac_check_mkdirat函数声明")
        
        func_start = find_do_mkdirat_function(content)
        func_end = find_function_end(content, func_start)
        
        var_decl_pos, security_check_pos = find_insertion_points(content, func_start)
        
        fmac_status_decl = '\n\tint fmac_status;'
        
        if 'fmac_status' not in content[func_start:func_end]:
            content = content[:var_decl_pos] + fmac_status_decl + content[var_decl_pos:]
            security_check_pos += len(fmac_status_decl)
        
        security_check_code = '''
\tfmac_status = fmac_check_mkdirat(name->name);
\tif (fmac_status) {
\t\treturn fmac_status;
\t}
'''
        if 'fmac_check_mkdirat' not in content[func_start:func_end]:
            content = content[:security_check_pos] + security_check_code + content[security_check_pos:]
            print("已插入fmac安全检查调用")
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print("文件修改完成!")
        
        return True
        
    except Exception as e:
        print(f"错误: {e}")
        print("恢复备份文件...")
        shutil.copy2(backup_path, file_path)
        return False

def verify_changes(file_path):
    """验证修改是否正确"""
    print("\n验证修改...")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    checks = [
        ('函数声明', 'int fmac_check_mkdirat(const char __user *pathname);'),
        ('变量声明', 'int fmac_status;'),
        ('安全检查调用', 'fmac_check_mkdirat(name->name)')
    ]
    
    all_passed = True
    for check_name, check_pattern in checks:
        if check_pattern in content:
            print(f"✓ {check_name} - 存在")
        else:
            print(f"✗ {check_name} - 缺失")
            all_passed = False
    
    return all_passed

def main():
    if len(sys.argv) != 2:
        print("用法: python3 adapt_namei.py <path_to_namei.c>")
        print("示例: python3 adapt_namei.py /path/to/linux-5.15/fs/namei.c")
        sys.exit(1)
    
    file_path = sys.argv[1]
    
    if not os.path.exists(file_path):
        print(f"错误: 文件不存在 {file_path}")
        sys.exit(1)
    
    print("Linux内核5.15 namei.c适配脚本")
    print("=" * 50)
    
    # 执行修改
    success = insert_fmac_check(file_path)
    
    if success:
        # 验证修改
        verify_success = verify_changes(file_path)
        
        if verify_success:
            print("\n🎉 适配完成! 文件已成功修改。")
            print("⚠️  注意: 请确保fmac_check_mkdirat函数已实现")
            print("⚠️  注意: 建议编译测试以确保兼容性")
        else:
            print("\n⚠️  适配完成，但部分验证失败，请手动检查")
    else:
        print("\n❌ 适配失败，文件已恢复原状")

if __name__ == "__main__":
    main()