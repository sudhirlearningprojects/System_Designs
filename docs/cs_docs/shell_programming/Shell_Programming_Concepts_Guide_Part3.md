# Shell Programming Concepts Guide - Part 3

*Basic UNIX Commands, Advanced Features, and Practice Questions*

## Table of Contents (Part 3)
8. [Basic UNIX Commands](#unix-commands)
9. [Advanced Shell Features](#advanced-features)
10. [MCQ Practice Questions](#mcq-questions)

---

## 8. Basic UNIX Commands {#unix-commands}

### File and Directory Operations

```bash
#!/bin/bash
# File and directory operations

# Directory navigation
echo "=== Directory Navigation ==="
pwd                    # Print working directory
ls                     # List files
ls -la                 # List with details and hidden files
ls -lh                 # List with human-readable sizes
cd /tmp                # Change directory
cd ~                   # Go to home directory
cd -                   # Go to previous directory

# Directory creation and removal
echo -e "\n=== Directory Management ==="
mkdir new_directory                    # Create directory
mkdir -p path/to/nested/directory     # Create nested directories
rmdir empty_directory                 # Remove empty directory
rm -rf directory_with_content         # Remove directory and contents

# File operations
echo -e "\n=== File Operations ==="
touch newfile.txt                     # Create empty file
cp source.txt destination.txt         # Copy file
cp -r source_dir dest_dir            # Copy directory recursively
mv oldname.txt newname.txt           # Move/rename file
rm file.txt                          # Remove file
rm -f file.txt                       # Force remove file
rm -i file.txt                       # Interactive remove

# File permissions
echo -e "\n=== File Permissions ==="
chmod 755 script.sh                  # Set permissions (rwxr-xr-x)
chmod +x script.sh                   # Add execute permission
chmod -w file.txt                    # Remove write permission
chown user:group file.txt            # Change ownership
chgrp group file.txt                 # Change group

# File information
echo -e "\n=== File Information ==="
stat file.txt                       # Detailed file information
file file.txt                       # File type information
du -h file.txt                      # File size
du -sh directory/                    # Directory size
df -h                               # Disk usage
```

### Text Processing Commands

```bash
#!/bin/bash
# Text processing commands

# Create sample file for demonstration
cat > sample.txt << EOF
apple
banana
cherry
apple
date
elderberry
fig
grape
apple
EOF

echo "=== Text Processing Commands ==="

# View file contents
echo "--- cat (display file) ---"
cat sample.txt

echo -e "\n--- head (first lines) ---"
head -3 sample.txt

echo -e "\n--- tail (last lines) ---"
tail -3 sample.txt

echo -e "\n--- wc (word count) ---"
wc sample.txt                        # Lines, words, characters
wc -l sample.txt                     # Line count only
wc -w sample.txt                     # Word count only

# Sorting and uniqueness
echo -e "\n--- sort ---"
sort sample.txt

echo -e "\n--- sort unique ---"
sort -u sample.txt

echo -e "\n--- uniq (requires sorted input) ---"
sort sample.txt | uniq -c            # Count occurrences

# Pattern searching
echo -e "\n--- grep (pattern search) ---"
grep "apple" sample.txt
grep -n "apple" sample.txt           # Show line numbers
grep -c "apple" sample.txt           # Count matches
grep -v "apple" sample.txt           # Invert match (exclude)
grep -i "APPLE" sample.txt           # Case insensitive

# Text manipulation
echo -e "\n--- cut (extract columns) ---"
echo "name:age:city" > data.txt
echo "john:25:newyork" >> data.txt
echo "jane:30:london" >> data.txt
cut -d: -f1 data.txt                 # Extract first field
cut -d: -f1,3 data.txt               # Extract fields 1 and 3

echo -e "\n--- awk (pattern processing) ---"
awk -F: '{print $1, $2}' data.txt   # Print fields 1 and 2
awk -F: '$2 > 25 {print $1}' data.txt # Conditional processing

echo -e "\n--- sed (stream editor) ---"
sed 's/apple/APPLE/' sample.txt      # Replace first occurrence
sed 's/apple/APPLE/g' sample.txt     # Replace all occurrences
sed '2d' sample.txt                  # Delete line 2
sed -n '2,4p' sample.txt             # Print lines 2-4

# Clean up
rm -f sample.txt data.txt
```

### Process Management

```bash
#!/bin/bash
# Process management commands

echo "=== Process Management ==="

# Process information
echo "--- ps (process status) ---"
ps                                   # Current user processes
ps aux                              # All processes with details
ps -ef                              # All processes (different format)

echo -e "\n--- top/htop (real-time processes) ---"
# top                               # Interactive process viewer
# htop                              # Enhanced process viewer

echo -e "\n--- pgrep/pkill (process search/kill) ---"
pgrep bash                          # Find process IDs by name
# pkill firefox                     # Kill processes by name

# Job control
echo -e "\n--- Job Control ---"
# sleep 100 &                       # Run in background
jobs                                # List active jobs
# fg %1                             # Bring job 1 to foreground
# bg %1                             # Send job 1 to background
# kill %1                           # Kill job 1

# Process monitoring
echo -e "\n--- Process Monitoring ---"
# nohup long_running_command &       # Run immune to hangups
# disown                            # Remove job from shell's job table

# System information
echo -e "\n--- System Information ---"
uptime                              # System uptime and load
who                                 # Logged in users
w                                   # Detailed user information
id                                  # Current user ID and groups
uname -a                           # System information
```

### File Search and Find

```bash
#!/bin/bash
# File search commands

echo "=== File Search Commands ==="

# find command
echo "--- find (search files) ---"
find . -name "*.txt"                # Find .txt files in current directory
find /home -name "*.log" 2>/dev/null # Find .log files in /home
find . -type f -size +1M            # Find files larger than 1MB
find . -type d -name "test*"        # Find directories starting with "test"
find . -mtime -7                    # Find files modified in last 7 days
find . -perm 755                    # Find files with specific permissions

# Advanced find usage
echo -e "\n--- Advanced find ---"
find . -name "*.tmp" -delete        # Find and delete .tmp files
find . -name "*.txt" -exec wc -l {} \; # Execute command on found files
find . -name "*.sh" -exec chmod +x {} \; # Make shell scripts executable

# locate command (if available)
echo -e "\n--- locate (fast file search) ---"
# updatedb                          # Update locate database
# locate filename                   # Fast search using database

# which and whereis
echo -e "\n--- which/whereis (command location) ---"
which bash                          # Find command location
which python3
whereis ls                          # Find command, source, manual locations

# type command
echo -e "\n--- type (command type) ---"
type ls                             # Show command type
type cd                             # Built-in command
type -a python                      # Show all locations
```

### Network and System Commands

```bash
#!/bin/bash
# Network and system commands

echo "=== Network Commands ==="

# Network information
echo "--- Network Information ---"
# ifconfig                          # Network interface configuration
# ip addr show                      # Modern network interface info
hostname                            # System hostname
# ping -c 3 google.com             # Test connectivity

# Network utilities
echo -e "\n--- Network Utilities ---"
# netstat -tuln                     # Show listening ports
# ss -tuln                          # Modern netstat alternative
# lsof -i :80                       # Show processes using port 80

# File transfer
echo -e "\n--- File Transfer ---"
# scp file.txt user@server:/path/   # Secure copy over SSH
# rsync -av source/ dest/           # Synchronize directories
# wget http://example.com/file.txt  # Download file
# curl -O http://example.com/file.txt # Download with curl

echo -e "\n=== System Commands ==="

# System monitoring
echo "--- System Monitoring ---"
free -h                             # Memory usage
df -h                               # Disk usage
# iostat                            # I/O statistics
# vmstat                            # Virtual memory statistics

# System control
echo -e "\n--- System Control ---"
date                                # Current date and time
# sudo systemctl status ssh         # Service status (systemd)
# sudo service ssh status           # Service status (SysV)
crontab -l                          # List cron jobs
# sudo crontab -e                   # Edit system cron jobs

# Archive and compression
echo -e "\n--- Archive and Compression ---"
# tar -czf archive.tar.gz directory/ # Create compressed archive
# tar -xzf archive.tar.gz            # Extract compressed archive
# zip -r archive.zip directory/      # Create zip archive
# unzip archive.zip                  # Extract zip archive
```

### Text Editors and Viewing

```bash
#!/bin/bash
# Text editors and file viewing

echo "=== Text Editors and Viewing ==="

# File viewing
echo "--- File Viewing ---"
# less filename                     # Page through file
# more filename                     # Page through file (basic)
# cat -n filename                   # Display with line numbers

# Text editors
echo -e "\n--- Text Editors ---"
# nano filename                     # Simple text editor
# vim filename                      # Vi/Vim editor
# emacs filename                    # Emacs editor

# Quick file editing
echo -e "\n--- Quick Editing ---"
# echo "new content" > file.txt     # Overwrite file
# echo "append content" >> file.txt # Append to file

# Here document for multi-line content
cat > config.txt << 'EOF'
# Configuration file
server=localhost
port=8080
debug=true
EOF

echo "Created config file:"
cat config.txt
rm config.txt
```

---

## 9. Advanced Shell Features {#advanced-features}

### Regular Expressions

```bash
#!/bin/bash
# Regular expressions in shell

echo "=== Regular Expressions ==="

# Test data
emails=("user@example.com" "invalid.email" "test@domain.co.uk" "bad@")
phones=("123-456-7890" "555.123.4567" "(555) 123-4567" "invalid")
ips=("192.168.1.1" "10.0.0.1" "256.1.1.1" "192.168.1")

# Email validation
echo "--- Email Validation ---"
email_regex="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
for email in "${emails[@]}"; do
    if [[ "$email" =~ $email_regex ]]; then
        echo "✓ Valid email: $email"
    else
        echo "✗ Invalid email: $email"
    fi
done

# Phone number validation
echo -e "\n--- Phone Validation ---"
phone_regex="^(\([0-9]{3}\)|[0-9]{3})[-.][0-9]{3}[-.][0-9]{4}$"
for phone in "${phones[@]}"; do
    if [[ "$phone" =~ $phone_regex ]]; then
        echo "✓ Valid phone: $phone"
    else
        echo "✗ Invalid phone: $phone"
    fi
done

# IP address validation
echo -e "\n--- IP Validation ---"
ip_regex="^([0-9]{1,3}\.){3}[0-9]{1,3}$"
for ip in "${ips[@]}"; do
    if [[ "$ip" =~ $ip_regex ]]; then
        # Additional check for valid IP ranges
        IFS='.' read -ra octets <<< "$ip"
        valid=true
        for octet in "${octets[@]}"; do
            if [[ $octet -gt 255 ]]; then
                valid=false
                break
            fi
        done
        if $valid; then
            echo "✓ Valid IP: $ip"
        else
            echo "✗ Invalid IP (out of range): $ip"
        fi
    else
        echo "✗ Invalid IP format: $ip"
    fi
done

# Pattern extraction
echo -e "\n--- Pattern Extraction ---"
log_line="2024-01-15 10:30:45 ERROR [UserService] Failed login attempt for user: john@example.com"
if [[ "$log_line" =~ ([0-9]{4}-[0-9]{2}-[0-9]{2})\ ([0-9]{2}:[0-9]{2}:[0-9]{2})\ ([A-Z]+)\ \[([^\]]+)\]\ (.+) ]]; then
    echo "Date: ${BASH_REMATCH[1]}"
    echo "Time: ${BASH_REMATCH[2]}"
    echo "Level: ${BASH_REMATCH[3]}"
    echo "Component: ${BASH_REMATCH[4]}"
    echo "Message: ${BASH_REMATCH[5]}"
fi
```

### Process Substitution and Pipes

```bash
#!/bin/bash
# Process substitution and advanced piping

echo "=== Process Substitution ==="

# Create test files
echo -e "apple\nbanana\ncherry" > fruits1.txt
echo -e "banana\ncherry\ndate" > fruits2.txt

# Compare files using process substitution
echo "--- File Comparison ---"
diff <(sort fruits1.txt) <(sort fruits2.txt)

# Multiple input sources
echo -e "\n--- Multiple Inputs ---"
paste <(echo -e "Name\nJohn\nJane") <(echo -e "Age\n25\n30") <(echo -e "City\nNY\nLA")

# Command substitution vs process substitution
echo -e "\n--- Command vs Process Substitution ---"
# Command substitution: $(command) or `command`
current_date=$(date)
echo "Current date: $current_date"

# Process substitution: <(command) or >(command)
# Useful when you need a filename instead of command output
while read -r line; do
    echo "Processing: $line"
done < <(ls -1)

# Advanced piping
echo -e "\n=== Advanced Piping ==="

# Tee - split output to multiple destinations
echo "--- Tee Command ---"
echo "This goes to both file and stdout" | tee output.txt

# Pipeline with error handling
echo -e "\n--- Pipeline Error Handling ---"
set -o pipefail  # Make pipeline fail if any command fails
if echo "test data" | grep "data" | wc -l > /dev/null; then
    echo "Pipeline succeeded"
else
    echo "Pipeline failed"
fi

# Named pipes (FIFOs)
echo -e "\n--- Named Pipes ---"
mkfifo mypipe
echo "Hello from pipe" > mypipe &
read message < mypipe
echo "Received: $message"
rm mypipe

# Clean up
rm -f fruits1.txt fruits2.txt output.txt
```

### Signal Handling and Traps

```bash
#!/bin/bash
# Signal handling and traps

echo "=== Signal Handling ==="

# Cleanup function
cleanup() {
    echo "Cleaning up..."
    rm -f temp_file.txt
    echo "Cleanup completed"
    exit 0
}

# Trap signals
trap cleanup EXIT      # Run cleanup on script exit
trap cleanup INT       # Run cleanup on Ctrl+C (SIGINT)
trap cleanup TERM      # Run cleanup on SIGTERM

# Create temporary file
touch temp_file.txt
echo "Temporary file created"

# Signal handling example
handle_sigusr1() {
    echo "Received SIGUSR1 signal"
    echo "Current time: $(date)"
}

trap handle_sigusr1 USR1

echo "Script PID: $$"
echo "Send SIGUSR1 with: kill -USR1 $$"
echo "Press Ctrl+C to test cleanup"

# Simulate work
for i in {1..10}; do
    echo "Working... $i"
    sleep 1
done

echo "Work completed normally"
```

### Advanced Parameter Expansion

```bash
#!/bin/bash
# Advanced parameter expansion

echo "=== Parameter Expansion ==="

# Variable setup
filename="document.pdf"
path="/home/user/documents/report.txt"
empty_var=""
unset_var

# Length
echo "--- String Length ---"
echo "Length of filename: ${#filename}"

# Substring extraction
echo -e "\n--- Substring Extraction ---"
echo "First 3 chars: ${filename:0:3}"
echo "From position 4: ${filename:4}"
echo "Last 3 chars: ${filename: -3}"

# Pattern removal
echo -e "\n--- Pattern Removal ---"
echo "Remove shortest match from beginning: ${path#*/}"
echo "Remove longest match from beginning: ${path##*/}"
echo "Remove shortest match from end: ${path%/*}"
echo "Remove longest match from end: ${path%%/*}"

# Pattern replacement
echo -e "\n--- Pattern Replacement ---"
text="hello world hello"
echo "Replace first: ${text/hello/hi}"
echo "Replace all: ${text//hello/hi}"
echo "Replace at beginning: ${text/#hello/hi}"
echo "Replace at end: ${text/%hello/hi}"

# Case modification
echo -e "\n--- Case Modification ---"
mixed_case="Hello World"
echo "Uppercase: ${mixed_case^^}"
echo "Lowercase: ${mixed_case,,}"
echo "First char upper: ${mixed_case^}"
echo "First char lower: ${mixed_case,}"

# Default values
echo -e "\n--- Default Values ---"
echo "Use default if unset: ${unset_var:-default_value}"
echo "Use default if unset or empty: ${empty_var:-default_value}"
echo "Assign default if unset: ${unset_var:=assigned_value}"
echo "unset_var is now: $unset_var"

# Error on unset
echo -e "\n--- Error Handling ---"
# echo "This would error: ${undefined_var:?Variable is required}"

# Indirect expansion
echo -e "\n--- Indirect Expansion ---"
var_name="filename"
echo "Indirect access: ${!var_name}"

# Array expansion
echo -e "\n--- Array Expansion ---"
array=("apple" "banana" "cherry")
echo "All elements: ${array[@]}"
echo "All indices: ${!array[@]}"
echo "Array length: ${#array[@]}"
```

### Command Line Processing

```bash
#!/bin/bash
# Advanced command line processing

usage() {
    cat << EOF
Usage: $0 [OPTIONS] [ARGUMENTS]

OPTIONS:
    -h, --help              Show this help
    -v, --verbose           Verbose output
    -q, --quiet             Quiet mode
    -f, --file FILE         Input file
    -o, --output FILE       Output file
    -n, --number NUM        Number parameter
    -l, --list ITEM         List item (can be repeated)
    --config FILE           Configuration file
    --dry-run               Show what would be done
    --force                 Force operation

EXAMPLES:
    $0 --file input.txt --output result.txt
    $0 -v --number 42 --list item1 --list item2
    $0 --config config.ini --dry-run
EOF
}

# Initialize variables
verbose=false
quiet=false
input_file=""
output_file=""
number=""
list_items=()
config_file=""
dry_run=false
force=false

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                usage
                exit 0
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -q|--quiet)
                quiet=true
                shift
                ;;
            -f|--file)
                input_file="$2"
                shift 2
                ;;
            -o|--output)
                output_file="$2"
                shift 2
                ;;
            -n|--number)
                number="$2"
                shift 2
                ;;
            -l|--list)
                list_items+=("$2")
                shift 2
                ;;
            --config)
                config_file="$2"
                shift 2
                ;;
            --config=*)
                config_file="${1#*=}"
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --force)
                force=true
                shift
                ;;
            --)
                shift
                break
                ;;
            -*)
                echo "Error: Unknown option $1" >&2
                usage
                exit 1
                ;;
            *)
                break
                ;;
        esac
    done
    
    # Store remaining arguments
    remaining_args=("$@")
}

# Validation function
validate_args() {
    local errors=0
    
    if [[ $verbose == true && $quiet == true ]]; then
        echo "Error: Cannot use both --verbose and --quiet" >&2
        ((errors++))
    fi
    
    if [[ -n "$input_file" && ! -f "$input_file" ]]; then
        echo "Error: Input file '$input_file' does not exist" >&2
        ((errors++))
    fi
    
    if [[ -n "$number" && ! "$number" =~ ^[0-9]+$ ]]; then
        echo "Error: Number must be a positive integer" >&2
        ((errors++))
    fi
    
    return $errors
}

# Main function
main() {
    parse_args "$@"
    
    if ! validate_args; then
        exit 1
    fi
    
    # Display parsed options
    echo "=== Parsed Options ==="
    echo "Verbose: $verbose"
    echo "Quiet: $quiet"
    echo "Input file: ${input_file:-Not specified}"
    echo "Output file: ${output_file:-Not specified}"
    echo "Number: ${number:-Not specified}"
    echo "List items: ${list_items[*]}"
    echo "Config file: ${config_file:-Not specified}"
    echo "Dry run: $dry_run"
    echo "Force: $force"
    echo "Remaining args: ${remaining_args[*]}"
    
    # Process based on options
    if [[ $dry_run == true ]]; then
        echo "DRY RUN: Would process with above options"
    else
        echo "Processing with above options..."
    fi
}

# Run main function with all arguments
main "$@"
```

This completes Part 2 covering loops, functions, return values, and basic UNIX commands with comprehensive examples and practical implementations.