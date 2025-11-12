# Shell Programming Concepts Guide - Part 2

*Loops, Functions, Return Values, and Basic UNIX Commands*

## Table of Contents (Part 2)
6. [Loops](#loops)
7. [Functions and Return Values](#functions)
8. [Basic UNIX Commands](#unix-commands)
9. [Advanced Shell Features](#advanced-features)

---

## 6. Loops {#loops}

### For Loops

```bash
#!/bin/bash
# For loop examples

# Basic for loop with list
echo "Fruits:"
for fruit in apple banana orange grape; do
    echo "  - $fruit"
done

# For loop with array
colors=("red" "green" "blue" "yellow")
echo "Colors:"
for color in "${colors[@]}"; do
    echo "  - $color"
done

# For loop with command substitution
echo "Files in current directory:"
for file in $(ls); do
    echo "  - $file"
done

# For loop with glob patterns
echo "Text files:"
for file in *.txt; do
    if [[ -f "$file" ]]; then
        echo "  - $file"
    fi
done

# C-style for loop
echo "Numbers 1-10:"
for ((i=1; i<=10; i++)); do
    echo "Number: $i"
done

# For loop with step
echo "Even numbers 2-20:"
for ((i=2; i<=20; i+=2)); do
    echo "Even: $i"
done

# For loop with range (Bash 4+)
echo "Range 1-5:"
for i in {1..5}; do
    echo "Value: $i"
done

# For loop with range and step
echo "Range 0-20 step 5:"
for i in {0..20..5}; do
    echo "Step: $i"
done
```

### While Loops

```bash
#!/bin/bash
# While loop examples

# Basic while loop
counter=1
echo "Counting to 5:"
while [[ $counter -le 5 ]]; do
    echo "Count: $counter"
    ((counter++))
done

# While loop reading file
echo "Reading file line by line:"
while IFS= read -r line; do
    echo "Line: $line"
done < /etc/passwd

# While loop with user input
echo "Enter numbers (0 to quit):"
while true; do
    read -p "Number: " num
    if [[ $num -eq 0 ]]; then
        echo "Goodbye!"
        break
    fi
    echo "You entered: $num"
done

# While loop with condition check
password=""
attempts=0
max_attempts=3

while [[ "$password" != "secret" && $attempts -lt $max_attempts ]]; do
    read -s -p "Enter password: " password
    echo
    ((attempts++))
    
    if [[ "$password" != "secret" ]]; then
        echo "Wrong password. Attempts left: $((max_attempts - attempts))"
    fi
done

if [[ "$password" == "secret" ]]; then
    echo "Access granted!"
else
    echo "Access denied. Too many attempts."
fi

# Infinite loop with break condition
echo "Processing files..."
while true; do
    # Simulate processing
    echo "Processing..."
    sleep 1
    
    # Break condition
    if [[ $(date +%S) -eq 0 ]]; then
        echo "Processing complete at top of minute"
        break
    fi
done
```

### Until Loops

```bash
#!/bin/bash
# Until loop examples

# Basic until loop
counter=1
echo "Until counter reaches 6:"
until [[ $counter -gt 5 ]]; do
    echo "Counter: $counter"
    ((counter++))
done

# Until loop waiting for file
echo "Waiting for file to be created..."
until [[ -f "signal.txt" ]]; do
    echo "Still waiting..."
    sleep 2
done
echo "File found!"

# Until loop with service check
check_service() {
    # Simulate service check
    return $((RANDOM % 2))
}

echo "Waiting for service to be ready..."
until check_service; do
    echo "Service not ready, retrying..."
    sleep 1
done
echo "Service is ready!"

# Until loop with timeout
timeout=10
elapsed=0

until [[ -f "important_file.txt" ]] || [[ $elapsed -ge $timeout ]]; do
    echo "Waiting for file... ($elapsed/$timeout seconds)"
    sleep 1
    ((elapsed++))
done

if [[ -f "important_file.txt" ]]; then
    echo "File found!"
else
    echo "Timeout reached, file not found"
fi
```

### Loop Control

```bash
#!/bin/bash
# Loop control: break and continue

# Break example
echo "Finding first even number greater than 10:"
for ((i=1; i<=20; i++)); do
    if [[ $((i % 2)) -eq 0 && $i -gt 10 ]]; then
        echo "Found: $i"
        break
    fi
done

# Continue example
echo "Odd numbers from 1-10:"
for ((i=1; i<=10; i++)); do
    if [[ $((i % 2)) -eq 0 ]]; then
        continue
    fi
    echo "Odd: $i"
done

# Nested loops with labeled break
echo "Finding coordinates:"
outer_loop: for ((x=1; x<=5; x++)); do
    for ((y=1; y<=5; y++)); do
        if [[ $((x * y)) -eq 12 ]]; then
            echo "Found coordinates: ($x, $y)"
            break outer_loop
        fi
    done
done

# Multiple conditions with continue
echo "Processing numbers 1-20:"
for ((i=1; i<=20; i++)); do
    # Skip multiples of 3
    if [[ $((i % 3)) -eq 0 ]]; then
        continue
    fi
    
    # Skip numbers containing 7
    if [[ "$i" == *7* ]]; then
        continue
    fi
    
    echo "Processing: $i"
done
```

### Practical Loop Examples

```bash
#!/bin/bash
# Practical loop applications

# File processing loop
process_log_files() {
    echo "Processing log files..."
    for logfile in /var/log/*.log; do
        if [[ -r "$logfile" ]]; then
            echo "Processing: $logfile"
            # Count lines
            lines=$(wc -l < "$logfile")
            echo "  Lines: $lines"
            
            # Count errors
            errors=$(grep -c "ERROR" "$logfile" 2>/dev/null || echo 0)
            echo "  Errors: $errors"
        fi
    done
}

# Backup multiple directories
backup_directories() {
    local backup_base="/backup"
    local dirs=("/home/user/documents" "/home/user/pictures" "/etc")
    
    for dir in "${dirs[@]}"; do
        if [[ -d "$dir" ]]; then
            backup_name="$(basename "$dir")_$(date +%Y%m%d)"
            echo "Backing up $dir to $backup_base/$backup_name"
            # tar -czf "$backup_base/$backup_name.tar.gz" "$dir"
        else
            echo "Warning: Directory $dir not found"
        fi
    done
}

# Monitor system resources
monitor_system() {
    local max_iterations=10
    local iteration=0
    
    while [[ $iteration -lt $max_iterations ]]; do
        echo "=== System Monitor ($(date)) ==="
        
        # CPU usage
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
        echo "CPU Usage: ${cpu_usage}%"
        
        # Memory usage
        memory_info=$(free | grep Mem)
        total_mem=$(echo $memory_info | awk '{print $2}')
        used_mem=$(echo $memory_info | awk '{print $3}')
        mem_percent=$((used_mem * 100 / total_mem))
        echo "Memory Usage: ${mem_percent}%"
        
        # Disk usage
        disk_usage=$(df -h / | awk 'NR==2 {print $5}')
        echo "Disk Usage: $disk_usage"
        
        echo "---"
        sleep 5
        ((iteration++))
    done
}

# Batch file rename
batch_rename() {
    local pattern="$1"
    local replacement="$2"
    
    echo "Renaming files: $pattern -> $replacement"
    
    for file in *"$pattern"*; do
        if [[ -f "$file" ]]; then
            new_name="${file/$pattern/$replacement}"
            echo "Renaming: $file -> $new_name"
            # mv "$file" "$new_name"
        fi
    done
}

# Example usage
# process_log_files
# backup_directories
# monitor_system
# batch_rename "old" "new"
```

---

## 7. Functions and Return Values {#functions}

### Basic Functions

```bash
#!/bin/bash
# Basic function examples

# Simple function
greet() {
    echo "Hello, World!"
}

# Function with parameters
greet_user() {
    local name="$1"
    echo "Hello, $name!"
}

# Function with multiple parameters
calculate_area() {
    local length="$1"
    local width="$2"
    local area=$((length * width))
    echo "Area: $area"
}

# Function with default parameters
greet_with_default() {
    local name="${1:-Guest}"
    local greeting="${2:-Hello}"
    echo "$greeting, $name!"
}

# Call functions
greet
greet_user "Alice"
calculate_area 10 5
greet_with_default
greet_with_default "Bob"
greet_with_default "Charlie" "Hi"
```

### Return Values and Exit Codes

```bash
#!/bin/bash
# Functions with return values

# Function returning exit code
is_even() {
    local number="$1"
    if [[ $((number % 2)) -eq 0 ]]; then
        return 0  # Success (true)
    else
        return 1  # Failure (false)
    fi
}

# Function returning value via echo
get_square() {
    local number="$1"
    local result=$((number * number))
    echo "$result"
}

# Function with multiple return values
get_file_info() {
    local file="$1"
    
    if [[ ! -f "$file" ]]; then
        return 1
    fi
    
    local size=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
    local lines=$(wc -l < "$file")
    local words=$(wc -w < "$file")
    
    echo "$size $lines $words"
    return 0
}

# Function with error handling
divide() {
    local dividend="$1"
    local divisor="$2"
    
    if [[ $divisor -eq 0 ]]; then
        echo "Error: Division by zero" >&2
        return 1
    fi
    
    local result=$((dividend / divisor))
    echo "$result"
    return 0
}

# Using functions with return values
echo "Testing return values:"

# Test is_even function
for num in 2 3 4 5; do
    if is_even "$num"; then
        echo "$num is even"
    else
        echo "$num is odd"
    fi
done

# Test get_square function
number=7
square=$(get_square "$number")
echo "Square of $number is $square"

# Test get_file_info function
if info=$(get_file_info "/etc/passwd"); then
    read -r size lines words <<< "$info"
    echo "File info - Size: $size, Lines: $lines, Words: $words"
else
    echo "Could not get file info"
fi

# Test divide function
if result=$(divide 10 2); then
    echo "10 / 2 = $result"
else
    echo "Division failed"
fi

if result=$(divide 10 0); then
    echo "10 / 0 = $result"
else
    echo "Division by zero error"
fi
```

### Local Variables and Scope

```bash
#!/bin/bash
# Variable scope in functions

# Global variable
global_var="I'm global"

function_with_local() {
    local local_var="I'm local"
    local global_var="I'm local override"
    
    echo "Inside function:"
    echo "  Local var: $local_var"
    echo "  Global var (overridden): $global_var"
}

function_without_local() {
    # This modifies the global variable
    global_var="Modified by function"
    new_global="Created in function"
}

echo "Before function calls:"
echo "Global var: $global_var"

function_with_local
echo "After function_with_local:"
echo "Global var: $global_var"

function_without_local
echo "After function_without_local:"
echo "Global var: $global_var"
echo "New global: $new_global"

# Function with readonly variables
function_with_readonly() {
    local -r readonly_var="Cannot change me"
    echo "Readonly var: $readonly_var"
    
    # This would cause an error:
    # readonly_var="Try to change"
}

function_with_readonly
```

### Advanced Function Features

```bash
#!/bin/bash
# Advanced function features

# Function with variable arguments
sum_all() {
    local total=0
    local count=0
    
    echo "Summing arguments: $@"
    
    for arg in "$@"; do
        if [[ "$arg" =~ ^[0-9]+$ ]]; then
            total=$((total + arg))
            ((count++))
        else
            echo "Warning: '$arg' is not a number, skipping"
        fi
    done
    
    echo "Sum of $count numbers: $total"
    return 0
}

# Function with named parameters (using associative array)
create_user() {
    local -A params
    
    # Parse named parameters
    while [[ $# -gt 0 ]]; do
        case $1 in
            --name)
                params[name]="$2"
                shift 2
                ;;
            --email)
                params[email]="$2"
                shift 2
                ;;
            --role)
                params[role]="$2"
                shift 2
                ;;
            *)
                echo "Unknown parameter: $1"
                return 1
                ;;
        esac
    done
    
    # Validate required parameters
    if [[ -z "${params[name]}" ]]; then
        echo "Error: --name is required"
        return 1
    fi
    
    # Set defaults
    params[role]="${params[role]:-user}"
    
    echo "Creating user:"
    echo "  Name: ${params[name]}"
    echo "  Email: ${params[email]:-Not provided}"
    echo "  Role: ${params[role]}"
}

# Recursive function
factorial() {
    local n="$1"
    
    if [[ $n -le 1 ]]; then
        echo 1
    else
        local prev=$(factorial $((n - 1)))
        echo $((n * prev))
    fi
}

# Function with callback
process_files() {
    local callback="$1"
    shift
    
    for file in "$@"; do
        if [[ -f "$file" ]]; then
            echo "Processing: $file"
            "$callback" "$file"
        fi
    done
}

# Callback functions
count_lines() {
    local file="$1"
    local lines=$(wc -l < "$file")
    echo "  Lines: $lines"
}

get_size() {
    local file="$1"
    local size=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
    echo "  Size: $size bytes"
}

# Function usage examples
echo "=== Sum All Example ==="
sum_all 1 2 3 4 5
sum_all 10 abc 20 def 30

echo -e "\n=== Create User Example ==="
create_user --name "John Doe" --email "john@example.com" --role "admin"
create_user --name "Jane Smith"

echo -e "\n=== Factorial Example ==="
for i in {1..5}; do
    result=$(factorial $i)
    echo "Factorial of $i: $result"
done

echo -e "\n=== Process Files Example ==="
# Create test files
echo "test content" > test1.txt
echo -e "line1\nline2\nline3" > test2.txt

process_files count_lines test1.txt test2.txt
echo "---"
process_files get_size test1.txt test2.txt

# Clean up
rm -f test1.txt test2.txt
```

### Function Libraries

```bash
#!/bin/bash
# Function library example

# File: utils.sh - Utility functions library

# String utilities
string_utils() {
    case "$1" in
        upper)
            echo "${2^^}"
            ;;
        lower)
            echo "${2,,}"
            ;;
        reverse)
            echo "$2" | rev
            ;;
        length)
            echo "${#2}"
            ;;
        *)
            echo "Usage: string_utils {upper|lower|reverse|length} <string>"
            return 1
            ;;
    esac
}

# Math utilities
math_utils() {
    case "$1" in
        add)
            echo $(($2 + $3))
            ;;
        subtract)
            echo $(($2 - $3))
            ;;
        multiply)
            echo $(($2 * $3))
            ;;
        divide)
            if [[ $3 -eq 0 ]]; then
                echo "Error: Division by zero" >&2
                return 1
            fi
            echo $(($2 / $3))
            ;;
        power)
            local result=1
            for ((i=0; i<$3; i++)); do
                result=$((result * $2))
            done
            echo "$result"
            ;;
        *)
            echo "Usage: math_utils {add|subtract|multiply|divide|power} <num1> <num2>"
            return 1
            ;;
    esac
}

# File utilities
file_utils() {
    case "$1" in
        backup)
            local file="$2"
            if [[ -f "$file" ]]; then
                cp "$file" "${file}.backup.$(date +%Y%m%d_%H%M%S)"
                echo "Backup created for $file"
            else
                echo "File not found: $file" >&2
                return 1
            fi
            ;;
        size)
            local file="$2"
            if [[ -f "$file" ]]; then
                stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null
            else
                echo "File not found: $file" >&2
                return 1
            fi
            ;;
        extension)
            echo "${2##*.}"
            ;;
        basename)
            echo "${2%.*}"
            ;;
        *)
            echo "Usage: file_utils {backup|size|extension|basename} <file>"
            return 1
            ;;
    esac
}

# Validation utilities
validate() {
    case "$1" in
        email)
            if [[ "$2" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
                return 0
            else
                return 1
            fi
            ;;
        number)
            if [[ "$2" =~ ^[0-9]+$ ]]; then
                return 0
            else
                return 1
            fi
            ;;
        ip)
            if [[ "$2" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
                return 0
            else
                return 1
            fi
            ;;
        *)
            echo "Usage: validate {email|number|ip} <value>"
            return 1
            ;;
    esac
}

# Example usage of utility functions
echo "=== String Utils ==="
string_utils upper "hello world"
string_utils lower "HELLO WORLD"
string_utils reverse "hello"
string_utils length "hello world"

echo -e "\n=== Math Utils ==="
math_utils add 10 5
math_utils multiply 7 8
math_utils power 2 3

echo -e "\n=== Validation ==="
if validate email "user@example.com"; then
    echo "Valid email"
else
    echo "Invalid email"
fi

if validate number "123"; then
    echo "Valid number"
else
    echo "Invalid number"
fi
```

This completes the loops and functions sections. The content covers comprehensive loop types, function definitions, return values, variable scope, and practical examples with utility libraries.