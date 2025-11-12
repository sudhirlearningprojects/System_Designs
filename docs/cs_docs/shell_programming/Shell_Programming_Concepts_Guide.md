# Shell Programming Concepts Guide - Part 1

*Comprehensive guide covering shell scripting fundamentals, variables, arguments, and control structures*

## Table of Contents (Part 1)
1. [Introduction to Shell Programming](#introduction)
2. [Shell Scripting Basics](#basics)
3. [Shell Variables](#variables)
4. [Shell Script Arguments](#arguments)
5. [If Statements](#if-statements)

---

## 1. Introduction to Shell Programming {#introduction}

### What is Shell Programming?

**Definition**: Shell programming is writing scripts using shell commands to automate tasks, manage system operations, and create powerful command-line tools.

**Types of Shells**:
- **Bash (Bourne Again Shell)**: Most common, default on Linux/macOS
- **Zsh (Z Shell)**: Enhanced features, default on newer macOS
- **Fish**: User-friendly with syntax highlighting
- **Dash**: Lightweight, POSIX-compliant
- **Ksh (Korn Shell)**: Advanced scripting features

### Why Shell Programming?

**Benefits**:
1. **Automation**: Automate repetitive tasks
2. **System Administration**: Manage files, processes, users
3. **Text Processing**: Parse logs, manipulate data
4. **Integration**: Glue different programs together
5. **Portability**: Run across Unix-like systems

---

## 2. Shell Scripting Basics {#basics}

### Creating Your First Script

```bash
#!/bin/bash
# This is a comment
# File: hello.sh

echo "Hello, World!"
echo "Today is: $(date)"
echo "Current user: $(whoami)"
echo "Current directory: $(pwd)"
```

**Script Structure**:
1. **Shebang**: `#!/bin/bash` - specifies interpreter
2. **Comments**: Lines starting with `#`
3. **Commands**: Shell commands and built-ins
4. **Variables**: Store and manipulate data

### Making Scripts Executable

```bash
# Make script executable
chmod +x hello.sh

# Run the script
./hello.sh

# Or run with bash explicitly
bash hello.sh
```

### Basic Script Template

```bash
#!/bin/bash
#
# Script Name: template.sh
# Description: Basic shell script template
# Author: Your Name
# Date: $(date +%Y-%m-%d)
# Version: 1.0
#

# Set strict mode
set -euo pipefail

# Global variables
SCRIPT_NAME=$(basename "$0")
SCRIPT_DIR=$(dirname "$0")

# Functions
usage() {
    echo "Usage: $SCRIPT_NAME [options]"
    echo "Options:"
    echo "  -h, --help    Show this help message"
    echo "  -v, --verbose Enable verbose output"
}

main() {
    echo "Script started at: $(date)"
    # Main script logic here
    echo "Script completed successfully"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -v|--verbose)
            set -x
            shift
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Call main function
main "$@"
```

### Input/Output Operations

```bash
#!/bin/bash
# Input/Output examples

# Reading user input
echo "Enter your name:"
read name
echo "Hello, $name!"

# Reading with prompt
read -p "Enter your age: " age
echo "You are $age years old"

# Reading password (hidden input)
read -s -p "Enter password: " password
echo
echo "Password entered (hidden)"

# Reading multiple values
echo "Enter first and last name:"
read first_name last_name
echo "Full name: $first_name $last_name"

# Reading from file
while IFS= read -r line; do
    echo "Line: $line"
done < input.txt

# Writing to file
echo "This is output" > output.txt
echo "This is appended" >> output.txt

# Here document
cat << EOF > config.txt
Server: localhost
Port: 8080
Database: mydb
EOF
```

### Error Handling

```bash
#!/bin/bash
# Error handling examples

# Exit on error
set -e

# Function to handle errors
error_handler() {
    echo "Error occurred in script at line $1"
    exit 1
}

# Trap errors
trap 'error_handler $LINENO' ERR

# Check command success
if command -v git >/dev/null 2>&1; then
    echo "Git is installed"
else
    echo "Git is not installed"
    exit 1
fi

# Check file existence
if [[ -f "important_file.txt" ]]; then
    echo "File exists"
else
    echo "File not found"
    exit 1
fi

# Try-catch equivalent
{
    # Commands that might fail
    risky_command
} || {
    # Error handling
    echo "Command failed, continuing..."
}
```

---

## 3. Shell Variables {#variables}

### Variable Declaration and Assignment

```bash
#!/bin/bash
# Variable examples

# Basic variable assignment (no spaces around =)
name="John Doe"
age=30
is_student=true

# Using variables
echo "Name: $name"
echo "Age: $age"
echo "Student: $is_student"

# Alternative syntax with braces
echo "Name: ${name}"
echo "Age: ${age}"

# Command substitution
current_date=$(date)
user_count=$(who | wc -l)
echo "Current date: $current_date"
echo "Users logged in: $user_count"

# Arithmetic operations
num1=10
num2=5
sum=$((num1 + num2))
product=$((num1 * num2))
echo "Sum: $sum"
echo "Product: $product"
```

### Variable Types and Scope

```bash
#!/bin/bash
# Variable types and scope

# Local variables (default)
local_var="I'm local"

# Global variables
declare -g global_var="I'm global"

# Read-only variables
declare -r readonly_var="Cannot change me"

# Integer variables
declare -i integer_var=42
integer_var="50"  # Automatically converted to integer

# Array variables
declare -a fruits=("apple" "banana" "orange")
declare -A colors=([red]="#FF0000" [green]="#00FF00" [blue]="#0000FF")

# Environment variables
export PATH_BACKUP="$PATH"
export CUSTOM_VAR="Available to child processes"

# Function with local variables
demo_function() {
    local func_var="I'm local to function"
    echo "Inside function: $func_var"
    echo "Global access: $global_var"
}

demo_function
# echo "$func_var"  # This would cause an error
```

### Special Variables

```bash
#!/bin/bash
# Special variables demonstration

echo "Script name: $0"
echo "First argument: $1"
echo "Second argument: $2"
echo "All arguments: $@"
echo "All arguments as string: $*"
echo "Number of arguments: $#"
echo "Process ID: $$"
echo "Exit status of last command: $?"
echo "Current user: $USER"
echo "Home directory: $HOME"
echo "Current working directory: $PWD"
echo "Previous working directory: $OLDPWD"
echo "Random number: $RANDOM"

# Demonstrating $? (exit status)
ls /nonexistent 2>/dev/null
echo "Exit status of ls command: $?"

true
echo "Exit status of true: $?"

false
echo "Exit status of false: $?"
```

### String Operations

```bash
#!/bin/bash
# String manipulation

text="Hello World Programming"

# String length
echo "Length: ${#text}"

# Substring extraction
echo "Substring (0,5): ${text:0:5}"
echo "Substring (6): ${text:6}"

# String replacement
echo "Replace first 'o': ${text/o/0}"
echo "Replace all 'o': ${text//o/0}"

# Case conversion
echo "Uppercase: ${text^^}"
echo "Lowercase: ${text,,}"
echo "First letter uppercase: ${text^}"

# String trimming
padded_text="   Hello World   "
echo "Original: '$padded_text'"
echo "Trimmed: '${padded_text// /}'"

# Pattern matching
if [[ $text == *"World"* ]]; then
    echo "Contains 'World'"
fi

# String concatenation
first_name="John"
last_name="Doe"
full_name="$first_name $last_name"
echo "Full name: $full_name"
```

### Arrays

```bash
#!/bin/bash
# Array operations

# Indexed arrays
fruits=("apple" "banana" "orange" "grape")

# Accessing elements
echo "First fruit: ${fruits[0]}"
echo "All fruits: ${fruits[@]}"
echo "Array length: ${#fruits[@]}"

# Adding elements
fruits+=("mango")
fruits[5]="kiwi"

# Iterating through array
echo "All fruits:"
for fruit in "${fruits[@]}"; do
    echo "  - $fruit"
done

# Array indices
echo "Array indices: ${!fruits[@]}"

# Associative arrays
declare -A person
person[name]="John Doe"
person[age]=30
person[city]="New York"

echo "Person details:"
for key in "${!person[@]}"; do
    echo "  $key: ${person[$key]}"
done

# Array slicing
numbers=(1 2 3 4 5 6 7 8 9 10)
echo "Numbers 2-5: ${numbers[@]:2:4}"
echo "Last 3 numbers: ${numbers[@]: -3}"
```

---

## 4. Shell Script Arguments {#arguments}

### Basic Argument Handling

```bash
#!/bin/bash
# File: args_demo.sh
# Basic argument handling

echo "Script name: $0"
echo "Total arguments: $#"

if [[ $# -eq 0 ]]; then
    echo "No arguments provided"
    echo "Usage: $0 <arg1> <arg2> ..."
    exit 1
fi

echo "Arguments provided:"
echo "First argument: $1"
echo "Second argument: $2"
echo "Third argument: $3"

echo "All arguments: $@"
echo "All arguments as single string: $*"

# Shift arguments
echo "After shift:"
shift
echo "New first argument: $1"
echo "Remaining arguments: $@"
```

### Advanced Argument Processing

```bash
#!/bin/bash
# Advanced argument processing with getopts

usage() {
    echo "Usage: $0 [-h] [-v] [-f file] [-n number] [arguments...]"
    echo "Options:"
    echo "  -h          Show help"
    echo "  -v          Verbose mode"
    echo "  -f file     Input file"
    echo "  -n number   Number parameter"
    exit 1
}

# Default values
verbose=false
input_file=""
number=0

# Process options
while getopts "hvf:n:" opt; do
    case $opt in
        h)
            usage
            ;;
        v)
            verbose=true
            echo "Verbose mode enabled"
            ;;
        f)
            input_file="$OPTARG"
            echo "Input file: $input_file"
            ;;
        n)
            number="$OPTARG"
            echo "Number: $number"
            ;;
        \?)
            echo "Invalid option: -$OPTARG"
            usage
            ;;
        :)
            echo "Option -$OPTARG requires an argument"
            usage
            ;;
    esac
done

# Shift processed options
shift $((OPTIND-1))

# Remaining arguments
echo "Remaining arguments: $@"

# Validate required parameters
if [[ -z "$input_file" ]]; then
    echo "Error: Input file is required"
    usage
fi

if [[ ! -f "$input_file" ]]; then
    echo "Error: File '$input_file' does not exist"
    exit 1
fi
```

### Long Options Processing

```bash
#!/bin/bash
# Long options processing

usage() {
    cat << EOF
Usage: $0 [OPTIONS] [ARGUMENTS]

Options:
    -h, --help          Show this help message
    -v, --verbose       Enable verbose output
    -f, --file FILE     Input file path
    -o, --output FILE   Output file path
    -c, --count NUM     Number of iterations
    --dry-run           Show what would be done without executing
    --config FILE       Configuration file

Examples:
    $0 --file input.txt --output result.txt
    $0 -v --count 10 --dry-run
EOF
}

# Default values
verbose=false
input_file=""
output_file=""
count=1
dry_run=false
config_file=""

# Parse long options
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
        -f|--file)
            input_file="$2"
            shift 2
            ;;
        -o|--output)
            output_file="$2"
            shift 2
            ;;
        -c|--count)
            count="$2"
            shift 2
            ;;
        --dry-run)
            dry_run=true
            shift
            ;;
        --config)
            config_file="$2"
            shift 2
            ;;
        --config=*)
            config_file="${1#*=}"
            shift
            ;;
        --)
            shift
            break
            ;;
        -*)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
        *)
            break
            ;;
    esac
done

# Display parsed options
echo "Parsed options:"
echo "  Verbose: $verbose"
echo "  Input file: $input_file"
echo "  Output file: $output_file"
echo "  Count: $count"
echo "  Dry run: $dry_run"
echo "  Config file: $config_file"
echo "  Remaining arguments: $@"
```

### Argument Validation

```bash
#!/bin/bash
# Argument validation functions

validate_file() {
    local file="$1"
    local description="$2"
    
    if [[ -z "$file" ]]; then
        echo "Error: $description is required"
        return 1
    fi
    
    if [[ ! -f "$file" ]]; then
        echo "Error: $description '$file' does not exist"
        return 1
    fi
    
    if [[ ! -r "$file" ]]; then
        echo "Error: $description '$file' is not readable"
        return 1
    fi
    
    return 0
}

validate_number() {
    local num="$1"
    local description="$2"
    local min="$3"
    local max="$4"
    
    if [[ -z "$num" ]]; then
        echo "Error: $description is required"
        return 1
    fi
    
    if ! [[ "$num" =~ ^[0-9]+$ ]]; then
        echo "Error: $description must be a positive integer"
        return 1
    fi
    
    if [[ -n "$min" && "$num" -lt "$min" ]]; then
        echo "Error: $description must be at least $min"
        return 1
    fi
    
    if [[ -n "$max" && "$num" -gt "$max" ]]; then
        echo "Error: $description must be at most $max"
        return 1
    fi
    
    return 0
}

validate_email() {
    local email="$1"
    local email_regex="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    
    if [[ -z "$email" ]]; then
        echo "Error: Email is required"
        return 1
    fi
    
    if ! [[ "$email" =~ $email_regex ]]; then
        echo "Error: Invalid email format"
        return 1
    fi
    
    return 0
}

# Example usage
input_file="$1"
count="$2"
email="$3"

validate_file "$input_file" "Input file" || exit 1
validate_number "$count" "Count" 1 100 || exit 1
validate_email "$email" || exit 1

echo "All validations passed!"
```

---

## 5. If Statements {#if-statements}

### Basic If Statements

```bash
#!/bin/bash
# Basic if statement examples

age=25

# Simple if statement
if [[ $age -ge 18 ]]; then
    echo "You are an adult"
fi

# If-else statement
if [[ $age -ge 21 ]]; then
    echo "You can drink alcohol in the US"
else
    echo "You cannot drink alcohol in the US"
fi

# If-elif-else statement
if [[ $age -lt 13 ]]; then
    echo "You are a child"
elif [[ $age -lt 20 ]]; then
    echo "You are a teenager"
elif [[ $age -lt 60 ]]; then
    echo "You are an adult"
else
    echo "You are a senior"
fi
```

### Comparison Operators

```bash
#!/bin/bash
# Comparison operators

num1=10
num2=20
str1="hello"
str2="world"

# Numeric comparisons
echo "Numeric comparisons:"
[[ $num1 -eq $num2 ]] && echo "$num1 equals $num2" || echo "$num1 does not equal $num2"
[[ $num1 -ne $num2 ]] && echo "$num1 not equals $num2"
[[ $num1 -lt $num2 ]] && echo "$num1 less than $num2"
[[ $num1 -le $num2 ]] && echo "$num1 less than or equal $num2"
[[ $num1 -gt $num2 ]] && echo "$num1 greater than $num2" || echo "$num1 not greater than $num2"
[[ $num1 -ge $num2 ]] && echo "$num1 greater than or equal $num2" || echo "$num1 not greater than or equal $num2"

# String comparisons
echo "String comparisons:"
[[ "$str1" == "$str2" ]] && echo "Strings are equal" || echo "Strings are not equal"
[[ "$str1" != "$str2" ]] && echo "Strings are different"
[[ "$str1" < "$str2" ]] && echo "$str1 comes before $str2 alphabetically"
[[ "$str1" > "$str2" ]] && echo "$str1 comes after $str2 alphabetically" || echo "$str1 does not come after $str2"

# String length and emptiness
[[ -z "$str1" ]] && echo "str1 is empty" || echo "str1 is not empty"
[[ -n "$str1" ]] && echo "str1 is not empty"
```

### File and Directory Tests

```bash
#!/bin/bash
# File and directory test conditions

file_path="/etc/passwd"
dir_path="/tmp"
nonexistent="/nonexistent"

echo "File tests:"
[[ -e "$file_path" ]] && echo "$file_path exists"
[[ -f "$file_path" ]] && echo "$file_path is a regular file"
[[ -d "$dir_path" ]] && echo "$dir_path is a directory"
[[ -r "$file_path" ]] && echo "$file_path is readable"
[[ -w "$dir_path" ]] && echo "$dir_path is writable"
[[ -x "/bin/ls" ]] && echo "/bin/ls is executable"
[[ -s "$file_path" ]] && echo "$file_path is not empty"

# File comparison
file1="file1.txt"
file2="file2.txt"
[[ "$file1" -nt "$file2" ]] && echo "$file1 is newer than $file2"
[[ "$file1" -ot "$file2" ]] && echo "$file1 is older than $file2"
[[ "$file1" -ef "$file2" ]] && echo "$file1 and $file2 are the same file"

# Create test files for demonstration
touch test_file.txt
chmod 755 test_file.txt

if [[ -f "test_file.txt" ]]; then
    echo "test_file.txt exists and is a regular file"
    
    if [[ -r "test_file.txt" ]]; then
        echo "File is readable"
    fi
    
    if [[ -w "test_file.txt" ]]; then
        echo "File is writable"
    fi
    
    if [[ -x "test_file.txt" ]]; then
        echo "File is executable"
    fi
fi
```

### Logical Operators

```bash
#!/bin/bash
# Logical operators in conditions

age=25
income=50000
has_license=true

# AND operator (&&)
if [[ $age -ge 18 && $has_license == true ]]; then
    echo "Can drive"
fi

# OR operator (||)
if [[ $age -ge 65 || $income -lt 20000 ]]; then
    echo "Eligible for discount"
else
    echo "Not eligible for discount"
fi

# NOT operator (!)
if [[ ! $age -lt 18 ]]; then
    echo "Not a minor"
fi

# Complex conditions
if [[ ($age -ge 18 && $age -le 65) && ($income -gt 30000 || $has_license == true) ]]; then
    echo "Eligible for premium service"
fi

# Multiple conditions with different operators
username="admin"
password="secret123"

if [[ -n "$username" && -n "$password" ]]; then
    if [[ "$username" == "admin" && "$password" == "secret123" ]]; then
        echo "Login successful"
    else
        echo "Invalid credentials"
    fi
else
    echo "Username and password required"
fi
```

### Pattern Matching

```bash
#!/bin/bash
# Pattern matching in if statements

filename="document.pdf"
email="user@example.com"
phone="123-456-7890"

# Wildcard patterns
if [[ "$filename" == *.pdf ]]; then
    echo "PDF file detected"
fi

if [[ "$filename" == *.txt || "$filename" == *.doc ]]; then
    echo "Text document"
else
    echo "Not a text document"
fi

# Regular expressions
if [[ "$email" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
    echo "Valid email format"
else
    echo "Invalid email format"
fi

if [[ "$phone" =~ ^[0-9]{3}-[0-9]{3}-[0-9]{4}$ ]]; then
    echo "Valid phone format"
else
    echo "Invalid phone format"
fi

# Case-insensitive matching
user_input="YES"
if [[ "${user_input,,}" == "yes" || "${user_input,,}" == "y" ]]; then
    echo "User confirmed"
fi

# Multiple pattern matching
file_extension="${filename##*.}"
case "$file_extension" in
    pdf|PDF)
        echo "PDF document"
        ;;
    txt|TXT)
        echo "Text file"
        ;;
    jpg|jpeg|png|gif)
        echo "Image file"
        ;;
    *)
        echo "Unknown file type"
        ;;
esac
```

### Nested If Statements

```bash
#!/bin/bash
# Nested if statements example

score=85
attendance=90
behavior="good"

echo "Student Evaluation System"
echo "Score: $score"
echo "Attendance: $attendance%"
echo "Behavior: $behavior"

if [[ $score -ge 60 ]]; then
    echo "Student passed"
    
    if [[ $score -ge 90 ]]; then
        echo "Grade: A"
        if [[ $attendance -ge 95 ]]; then
            echo "Perfect attendance bonus!"
        fi
    elif [[ $score -ge 80 ]]; then
        echo "Grade: B"
        if [[ $behavior == "excellent" ]]; then
            echo "Behavior bonus applied"
        fi
    elif [[ $score -ge 70 ]]; then
        echo "Grade: C"
    else
        echo "Grade: D"
    fi
    
    if [[ $attendance -lt 75 ]]; then
        echo "Warning: Low attendance"
        if [[ $attendance -lt 50 ]]; then
            echo "Critical: Attendance below 50%"
        fi
    fi
    
else
    echo "Student failed"
    
    if [[ $score -ge 50 ]]; then
        echo "Eligible for retest"
    else
        echo "Must repeat the course"
    fi
fi

# Final recommendation
if [[ $score -ge 80 && $attendance -ge 85 && $behavior == "good" ]]; then
    echo "Recommendation: Promote to next level"
elif [[ $score -ge 60 && $attendance -ge 75 ]]; then
    echo "Recommendation: Conditional promotion"
else
    echo "Recommendation: Additional support needed"
fi
```

This completes Part 1 of the Shell Programming guide, covering introduction, basics, variables, arguments, and if statements with comprehensive examples and practical implementations.