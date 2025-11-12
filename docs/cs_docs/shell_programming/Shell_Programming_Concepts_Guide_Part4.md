# Shell Programming Concepts Guide - Part 4

*MCQ Practice Questions and Summary*

## Table of Contents (Part 4)
10. [MCQ Practice Questions](#mcq-questions)
11. [Summary and Best Practices](#summary)

---

## 10. MCQ Practice Questions {#mcq-questions}

### Questions 1-10: Shell Basics

**1. Which line should be the first line of a bash script?**
a) #!/bin/sh
b) #!/bin/bash
c) #/bin/bash
d) #!bash

**Answer: b) #!/bin/bash**
*Explanation: The shebang `#!/bin/bash` tells the system which interpreter to use for the script.*

**2. How do you make a shell script executable?**
a) chmod +x script.sh
b) chmod 755 script.sh
c) Both a and b
d) exec script.sh

**Answer: c) Both a and b**
*Explanation: Both `chmod +x` and `chmod 755` make a script executable.*

**3. Which command displays the current working directory?**
a) cwd
b) pwd
c) cd
d) dir

**Answer: b) pwd**
*Explanation: `pwd` stands for "print working directory" and shows the current directory path.*

**4. What does the `$?` variable contain?**
a) Process ID of current script
b) Number of arguments
c) Exit status of last command
d) Current user name

**Answer: c) Exit status of last command**
*Explanation: `$?` contains the exit code (0-255) of the previously executed command.*

**5. How do you assign a value to a variable in bash?**
a) var = value
b) var=value
c) set var=value
d) $var=value

**Answer: b) var=value**
*Explanation: Variable assignment in bash requires no spaces around the equals sign.*

**6. Which operator is used for string comparison in bash?**
a) -eq
b) ==
c) =
d) Both b and c

**Answer: d) Both b and c**
*Explanation: Both `==` and `=` can be used for string comparison in bash.*

**7. What does `$#` represent in a shell script?**
a) Process ID
b) Number of arguments passed to script
c) Exit status
d) Current line number

**Answer: b) Number of arguments passed to script**
*Explanation: `$#` contains the count of command-line arguments passed to the script.*

**8. Which command is used to read user input in a script?**
a) input
b) get
c) read
d) scan

**Answer: c) read**
*Explanation: The `read` command is used to get input from the user or from a file.*

**9. What is the correct syntax for a basic if statement?**
a) if [ condition ]; then
b) if [[ condition ]]; then
c) Both a and b
d) if condition then

**Answer: c) Both a and b**
*Explanation: Both `[ ]` and `[[ ]]` can be used for conditions, with `[[ ]]` being more feature-rich.*

**10. How do you comment a line in a shell script?**
a) // comment
b) /* comment */
c) # comment
d) -- comment

**Answer: c) # comment**
*Explanation: Lines starting with `#` are treated as comments in shell scripts.*

### Questions 11-20: Variables and Arguments

**11. Which variable contains all command-line arguments as separate words?**
a) $*
b) $@
c) $#
d) $0

**Answer: b) $@**
*Explanation: `$@` expands to all arguments as separate words, while `$*` treats them as a single word.*

**12. How do you access the length of a string variable?**
a) ${#var}
b) $#var
c) length($var)
d) ${var#}

**Answer: a) ${#var}**
*Explanation: `${#var}` returns the length of the string stored in variable `var`.*

**13. What does `${var:-default}` do?**
a) Assigns default to var if var is unset
b) Returns default if var is unset or empty
c) Returns var if default is unset
d) Compares var with default

**Answer: b) Returns default if var is unset or empty**
*Explanation: This parameter expansion returns the default value if var is unset or empty.*

**14. Which command processes command-line options in shell scripts?**
a) getopt
b) getopts
c) Both a and b
d) optparse

**Answer: c) Both a and b**
*Explanation: Both `getopt` and `getopts` can process command-line options, with `getopts` being built-in.*

**15. How do you make a variable read-only?**
a) readonly var
b) declare -r var
c) Both a and b
d) const var

**Answer: c) Both a and b**
*Explanation: Both `readonly` and `declare -r` make variables read-only.*

**16. What is the scope of variables declared with `local`?**
a) Global scope
b) Function scope only
c) Script scope
d) System scope

**Answer: b) Function scope only**
*Explanation: Variables declared with `local` are only accessible within the function.*

**17. How do you create an array in bash?**
a) array=(item1 item2 item3)
b) declare -a array=(item1 item2 item3)
c) Both a and b
d) array[0]=item1; array[1]=item2

**Answer: c) Both a and b**
*Explanation: Arrays can be created using parentheses syntax or with `declare -a`.*

**18. Which syntax accesses all elements of an array?**
a) ${array[*]}
b) ${array[@]}
c) Both a and b
d) $array[*]

**Answer: c) Both a and b**
*Explanation: Both `${array[*]}` and `${array[@]}` access all array elements.*

**19. How do you get a substring from position 2 with length 3?**
a) ${var:2:3}
b) ${var[2:3]}
c) substr(var, 2, 3)
d) ${var(2,3)}

**Answer: a) ${var:2:3}**
*Explanation: `${var:start:length}` extracts a substring starting at position `start` with given `length`.*

**20. What does `shift` do in a shell script?**
a) Moves all arguments one position to the left
b) Moves all arguments one position to the right
c) Shifts bits in a number
d) Changes the current directory

**Answer: a) Moves all arguments one position to the left**
*Explanation: `shift` removes the first argument and shifts remaining arguments left.*

### Questions 21-30: Control Structures and Loops

**21. Which loop continues until a condition becomes true?**
a) for loop
b) while loop
c) until loop
d) do-while loop

**Answer: c) until loop**
*Explanation: `until` loop continues executing until the condition becomes true (opposite of while).*

**22. What is the correct syntax for a C-style for loop?**
a) for (i=0; i<10; i++)
b) for ((i=0; i<10; i++))
c) for i in (0..10)
d) for i=0 to 10

**Answer: b) for ((i=0; i<10; i++))**
*Explanation: C-style for loops in bash require double parentheses.*

**23. Which command exits from a loop?**
a) exit
b) break
c) continue
d) return

**Answer: b) break**
*Explanation: `break` exits from the current loop, while `continue` skips to the next iteration.*

**24. How do you create an infinite loop?**
a) while true
b) while :
c) while 1
d) All of the above

**Answer: d) All of the above**
*Explanation: `true`, `:`, and `1` all evaluate to true, creating infinite loops.*

**25. What does `continue` do in a loop?**
a) Exits the loop
b) Skips to the next iteration
c) Restarts the loop from beginning
d) Pauses the loop

**Answer: b) Skips to the next iteration**
*Explanation: `continue` skips the remaining code in the current iteration and moves to the next.*

**26. Which operator is used for numeric comparison "greater than"?**
a) >
b) -gt
c) Both a and b depending on context
d) gt

**Answer: c) Both a and b depending on context**
*Explanation: `>` is used in `[[ ]]` and `-gt` is used in `[ ]` for numeric comparison.*

**27. How do you check if a file exists?**
a) if [ -f filename ]
b) if [ -e filename ]
c) Both a and b
d) if exists filename

**Answer: c) Both a and b**
*Explanation: `-f` checks for regular files, `-e` checks for any file type existence.*

**28. What is the correct syntax for case statement?**
a) case $var in pattern) commands;; esac
b) switch $var { pattern: commands }
c) case $var of pattern: commands end
d) select $var in pattern do commands done

**Answer: a) case $var in pattern) commands;; esac**
*Explanation: Case statements use `case...in...esac` syntax with `;;` to separate cases.*

**29. Which loop is best for iterating over command-line arguments?**
a) for arg in "$@"
b) while [ $# -gt 0 ]
c) Both a and b
d) until [ $# -eq 0 ]

**Answer: c) Both a and b**
*Explanation: Both methods can iterate over arguments, with different use cases.*

**30. How do you test multiple conditions with AND logic?**
a) if [ cond1 ] && [ cond2 ]
b) if [[ cond1 && cond2 ]]
c) Both a and b
d) if [ cond1 -and cond2 ]

**Answer: c) Both a and b**
*Explanation: Both syntaxes work for AND logic in conditional statements.*

### Questions 31-40: Functions and Advanced Features

**31. How do you define a function in bash?**
a) function name() { commands; }
b) name() { commands; }
c) Both a and b
d) def name() { commands; }

**Answer: c) Both a and b**
*Explanation: Functions can be defined with or without the `function` keyword.*

**32. How do you return a value from a function?**
a) return value
b) echo value
c) Both a and b serve different purposes
d) output value

**Answer: c) Both a and b serve different purposes**
*Explanation: `return` sets exit status (0-255), `echo` outputs text that can be captured.*

**33. What does `$1` represent inside a function?**
a) First command-line argument to script
b) First argument passed to function
c) Process ID
d) Exit status

**Answer: b) First argument passed to function**
*Explanation: Inside functions, `$1`, `$2`, etc. refer to function arguments, not script arguments.*

**34. Which command substitution syntax is preferred?**
a) `command`
b) $(command)
c) Both are equivalent
d) ${command}

**Answer: b) $(command)**
*Explanation: `$(command)` is preferred as it's more readable and supports nesting better.*

**35. What is process substitution?**
a) <(command)
b) $(command)
c) `command`
d) >(command)

**Answer: a) <(command)**
*Explanation: Process substitution `<(command)` creates a temporary file-like interface for command output.*

**36. How do you handle signals in a shell script?**
a) trap 'commands' SIGNAL
b) catch SIGNAL 'commands'
c) on SIGNAL do commands
d) signal SIGNAL commands

**Answer: a) trap 'commands' SIGNAL**
*Explanation: The `trap` command is used to handle signals in shell scripts.*

**37. Which signal is sent by Ctrl+C?**
a) SIGTERM
b) SIGINT
c) SIGKILL
d) SIGHUP

**Answer: b) SIGINT**
*Explanation: Ctrl+C sends SIGINT (interrupt signal) to the process.*

**38. What does `set -e` do?**
a) Enables debugging
b) Exits script on any command failure
c) Enables verbose mode
d) Sets environment variables

**Answer: b) Exits script on any command failure**
*Explanation: `set -e` makes the script exit immediately if any command returns non-zero status.*

**39. How do you create a here document?**
a) << EOF ... EOF
b) < EOF ... EOF
c) >> EOF ... EOF
d) <> EOF ... EOF

**Answer: a) << EOF ... EOF**
*Explanation: Here documents use `<<` followed by a delimiter (commonly EOF).*

**40. What is the difference between `source` and executing a script?**
a) No difference
b) source runs in current shell, executing creates new shell
c) source is faster
d) source only works with bash scripts

**Answer: b) source runs in current shell, executing creates new shell**
*Explanation: `source` (or `.`) runs the script in the current shell environment.*

---

## 11. Summary and Best Practices {#summary}

### Key Concepts Summary

**Shell Scripting Fundamentals**:
- Always start scripts with appropriate shebang (`#!/bin/bash`)
- Use meaningful variable names and comments
- Make scripts executable with `chmod +x`
- Handle errors gracefully with proper exit codes

**Variables and Parameters**:
- No spaces around `=` in assignments
- Use `${var}` for clarity and to avoid ambiguity
- Understand the difference between `$*` and `$@`
- Use `local` for function variables
- Quote variables to handle spaces: `"$var"`

**Control Structures**:
- Use `[[ ]]` for enhanced conditional testing
- Understand numeric vs string comparisons
- Use appropriate loop types for different scenarios
- Implement proper error handling with `set -e`

**Functions**:
- Use functions for code reusability
- Return exit codes (0-255) with `return`
- Output values with `echo` for capture
- Use `local` variables to avoid conflicts

**UNIX Commands**:
- Master basic file operations (`ls`, `cp`, `mv`, `rm`)
- Understand text processing (`grep`, `sed`, `awk`, `sort`)
- Use pipes and redirection effectively
- Know process management commands (`ps`, `kill`, `jobs`)

### Best Practices

**Script Structure**:
```bash
#!/bin/bash
#
# Script: example.sh
# Purpose: Demonstrate best practices
# Author: Your Name
# Date: $(date +%Y-%m-%d)
#

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Global variables
readonly SCRIPT_NAME=$(basename "$0")
readonly SCRIPT_DIR=$(dirname "$0")

# Functions
usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] [ARGUMENTS]
    -h, --help    Show this help
    -v, --verbose Enable verbose output
EOF
}

main() {
    # Main script logic
    echo "Script execution completed successfully"
}

# Parse arguments and call main
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help) usage; exit 0 ;;
        -v|--verbose) set -x; shift ;;
        *) break ;;
    esac
done

main "$@"
```

**Error Handling**:
```bash
# Check command availability
command -v git >/dev/null 2>&1 || {
    echo "Error: git is required but not installed" >&2
    exit 1
}

# Check file existence
[[ -f "$config_file" ]] || {
    echo "Error: Config file not found: $config_file" >&2
    exit 1
}

# Trap for cleanup
cleanup() {
    rm -f "$temp_file"
    echo "Cleanup completed"
}
trap cleanup EXIT
```

**Variable Handling**:
```bash
# Always quote variables
cp "$source_file" "$destination_dir"

# Use parameter expansion
filename="${1:-default.txt}"
extension="${filename##*.}"
basename="${filename%.*}"

# Array handling
files=("*.txt" "*.log")
for file in "${files[@]}"; do
    [[ -f "$file" ]] && echo "Processing: $file"
done
```

**Function Design**:
```bash
# Good function design
validate_email() {
    local email="$1"
    local regex="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    
    [[ -n "$email" ]] || return 1
    [[ "$email" =~ $regex ]] || return 1
    
    return 0
}

# Usage
if validate_email "$user_email"; then
    echo "Valid email: $user_email"
else
    echo "Invalid email format" >&2
    exit 1
fi
```

### Common Pitfalls to Avoid

1. **Unquoted Variables**: Always quote variables containing spaces
2. **Spaces in Assignments**: `var=value` not `var = value`
3. **Exit Code Confusion**: Remember 0 = success, non-zero = failure
4. **Global Variables in Functions**: Use `local` to avoid conflicts
5. **Not Handling Errors**: Use `set -e` and check return codes
6. **Hardcoded Paths**: Use relative paths or make paths configurable
7. **Not Testing Edge Cases**: Test with empty inputs, special characters
8. **Poor Documentation**: Always comment complex logic

### Performance Tips

1. **Use Built-ins**: Prefer bash built-ins over external commands
2. **Minimize Subshells**: Avoid unnecessary command substitutions
3. **Efficient Loops**: Use appropriate loop types for the task
4. **Reduce I/O**: Batch file operations when possible
5. **Cache Results**: Store expensive computations in variables

### Security Considerations

1. **Input Validation**: Always validate user input
2. **Path Safety**: Use absolute paths or validate relative paths
3. **Temporary Files**: Use `mktemp` for secure temporary files
4. **Permissions**: Set appropriate file permissions
5. **Avoid eval**: Never use `eval` with user input

This comprehensive Shell Programming guide covers all fundamental and advanced concepts needed for effective shell scripting, from basic syntax to complex automation tasks. The guide provides practical examples, best practices, and thorough MCQ preparation for exams and interviews.