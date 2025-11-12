# Data Analytics Languages Guide - Part 1

*Comprehensive guide covering Python and R for data analytics from basics to advanced concepts*

## Table of Contents (Part 1)
1. [Introduction to Data Analytics Languages](#introduction)
2. [Python Basics](#python-basics)
3. [Lists and Data Structures](#lists-data-structures)
4. [Dictionaries and Sets](#dictionaries-sets)
5. [Regular Expressions (Regex)](#regex)

---

## 1. Introduction to Data Analytics Languages {#introduction}

### Python vs R Comparison

**Python**:
- **General Purpose**: Programming language with data science libraries
- **Syntax**: Clean, readable, beginner-friendly
- **Libraries**: NumPy, Pandas, Matplotlib, Scikit-learn, TensorFlow
- **Use Cases**: Web development, automation, machine learning, data analysis
- **Community**: Large, diverse community

**R**:
- **Statistical Focus**: Designed specifically for statistics and data analysis
- **Syntax**: Mathematical notation-like, domain-specific
- **Libraries**: ggplot2, dplyr, tidyr, caret, randomForest
- **Use Cases**: Statistical analysis, research, academic work, visualization
- **Community**: Strong academic and research community

### Setting Up Environment

**Python Setup**:
```python
# Essential libraries for data analytics
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import scipy.stats as stats
from sklearn import datasets, model_selection, metrics
import re
import json
import csv
```

**R Setup**:
```r
# Essential libraries for data analytics
library(dplyr)
library(ggplot2)
library(tidyr)
library(readr)
library(stringr)
library(lubridate)
library(caret)
library(corrplot)

# Install packages if needed
# install.packages(c("dplyr", "ggplot2", "tidyr", "readr"))
```

---

## 2. Python Basics {#python-basics}

### Variables and Data Types

**Basic Data Types**:
```python
# Numeric types
integer_var = 42
float_var = 3.14159
complex_var = 3 + 4j

# String type
string_var = "Data Analytics"
multiline_string = """This is a
multiline string for
data analysis"""

# Boolean type
boolean_var = True
is_data_clean = False

# None type
missing_value = None

# Type checking
print(type(integer_var))  # <class 'int'>
print(isinstance(float_var, float))  # True
```

**String Operations**:
```python
text = "Data Analytics with Python"

# String methods
print(text.upper())           # DATA ANALYTICS WITH PYTHON
print(text.lower())           # data analytics with python
print(text.title())           # Data Analytics With Python
print(text.replace("Python", "R"))  # Data Analytics with R

# String formatting
name = "Alice"
score = 95.5
print(f"Student {name} scored {score:.1f}%")  # f-string (Python 3.6+)
print("Student {} scored {:.1f}%".format(name, score))  # format method
print("Student %s scored %.1f%%" % (name, score))  # % formatting

# String slicing
print(text[0:4])     # Data
print(text[:4])      # Data
print(text[5:])      # Analytics with Python
print(text[-6:])     # Python
print(text[::2])     # Dt nlti ihPto
```

### Control Structures

**Conditional Statements**:
```python
score = 85

if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
elif score >= 70:
    grade = "C"
else:
    grade = "F"

print(f"Grade: {grade}")

# Ternary operator
status = "Pass" if score >= 60 else "Fail"

# Multiple conditions
age = 25
income = 50000
if age >= 18 and income > 30000:
    print("Eligible for loan")
```

**Loops**:
```python
# For loop with range
for i in range(5):
    print(f"Iteration {i}")

# For loop with list
fruits = ["apple", "banana", "cherry"]
for fruit in fruits:
    print(f"I like {fruit}")

# For loop with enumerate
for index, fruit in enumerate(fruits):
    print(f"{index}: {fruit}")

# While loop
count = 0
while count < 3:
    print(f"Count: {count}")
    count += 1

# List comprehension (Pythonic way)
squares = [x**2 for x in range(10)]
even_squares = [x**2 for x in range(10) if x % 2 == 0]
```

---

## 3. Lists and Data Structures {#lists-data-structures}

### Lists

**List Creation and Basic Operations**:
```python
# Creating lists
numbers = [1, 2, 3, 4, 5]
mixed_list = [1, "hello", 3.14, True]
empty_list = []
range_list = list(range(10))

# List operations
numbers.append(6)           # Add element at end
numbers.insert(0, 0)        # Insert at specific position
numbers.extend([7, 8, 9])   # Add multiple elements
numbers.remove(5)           # Remove first occurrence
popped = numbers.pop()      # Remove and return last element
numbers.clear()             # Remove all elements

# List methods
data = [3, 1, 4, 1, 5, 9, 2, 6]
print(data.count(1))        # Count occurrences: 2
print(data.index(4))        # Find index: 2
data.sort()                 # Sort in place
data.reverse()              # Reverse in place
```

**List Slicing and Indexing**:
```python
data = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

# Basic slicing
print(data[2:7])        # [2, 3, 4, 5, 6]
print(data[:5])         # [0, 1, 2, 3, 4]
print(data[5:])         # [5, 6, 7, 8, 9]
print(data[-3:])        # [7, 8, 9]
print(data[:-3])        # [0, 1, 2, 3, 4, 5, 6]

# Step slicing
print(data[::2])        # [0, 2, 4, 6, 8] - every 2nd element
print(data[1::2])       # [1, 3, 5, 7, 9] - every 2nd starting from index 1
print(data[::-1])       # [9, 8, 7, 6, 5, 4, 3, 2, 1, 0] - reverse

# Advanced slicing
print(data[2:8:2])      # [2, 4, 6] - from index 2 to 8, step 2
```

**List Comprehensions**:
```python
# Basic list comprehension
squares = [x**2 for x in range(10)]

# With condition
even_squares = [x**2 for x in range(10) if x % 2 == 0]

# Nested list comprehension
matrix = [[i*j for j in range(3)] for i in range(3)]
# Result: [[0, 0, 0], [0, 1, 2], [0, 2, 4]]

# String processing
words = ["hello", "world", "python", "data"]
lengths = [len(word) for word in words]
uppercase = [word.upper() for word in words if len(word) > 4]

# Flattening nested lists
nested = [[1, 2], [3, 4], [5, 6]]
flattened = [item for sublist in nested for item in sublist]
# Result: [1, 2, 3, 4, 5, 6]
```

### Tuples

**Tuple Operations**:
```python
# Creating tuples
coordinates = (10, 20)
single_element = (42,)  # Note the comma
empty_tuple = ()

# Tuple unpacking
x, y = coordinates
print(f"x: {x}, y: {y}")

# Multiple assignment
a, b, c = 1, 2, 3

# Swapping variables
a, b = b, a

# Named tuples
from collections import namedtuple
Point = namedtuple('Point', ['x', 'y'])
p = Point(10, 20)
print(p.x, p.y)  # 10 20
```

### NumPy Arrays

**Array Creation and Operations**:
```python
import numpy as np

# Creating arrays
arr1 = np.array([1, 2, 3, 4, 5])
arr2 = np.array([[1, 2, 3], [4, 5, 6]])
zeros = np.zeros((3, 4))
ones = np.ones((2, 3))
identity = np.eye(3)
random_arr = np.random.random((2, 3))

# Array properties
print(arr2.shape)       # (2, 3)
print(arr2.dtype)       # int64
print(arr2.size)        # 6
print(arr2.ndim)        # 2

# Array operations
arr = np.array([1, 2, 3, 4, 5])
print(arr + 10)         # [11 12 13 14 15]
print(arr * 2)          # [2 4 6 8 10]
print(arr ** 2)         # [1 4 9 16 25]

# Boolean indexing
print(arr[arr > 3])     # [4 5]

# Array slicing
matrix = np.array([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
print(matrix[0, :])     # [1 2 3] - first row
print(matrix[:, 1])     # [2 5 8] - second column
print(matrix[1:, 1:])   # [[5 6], [8 9]] - submatrix
```

---

## 4. Dictionaries and Sets {#dictionaries-sets}

### Dictionaries

**Dictionary Creation and Operations**:
```python
# Creating dictionaries
student = {
    "name": "Alice",
    "age": 22,
    "grade": "A",
    "subjects": ["Math", "Physics", "Chemistry"]
}

# Alternative creation methods
student2 = dict(name="Bob", age=23, grade="B")
student3 = dict([("name", "Charlie"), ("age", 24)])

# Dictionary operations
print(student["name"])          # Alice
print(student.get("age"))       # 22
print(student.get("height", "Not found"))  # Not found

# Adding/updating elements
student["height"] = 170
student.update({"weight": 65, "city": "New York"})

# Removing elements
del student["weight"]
popped_value = student.pop("city", "Not found")
last_item = student.popitem()   # Remove and return last item

# Dictionary methods
print(student.keys())           # dict_keys(['name', 'age', 'grade', 'subjects', 'height'])
print(student.values())         # dict_values(['Alice', 22, 'A', ['Math', 'Physics', 'Chemistry'], 170])
print(student.items())          # dict_items([...])
```

**Dictionary Comprehensions**:
```python
# Basic dictionary comprehension
squares = {x: x**2 for x in range(5)}
# Result: {0: 0, 1: 1, 2: 4, 3: 9, 4: 16}

# With condition
even_squares = {x: x**2 for x in range(10) if x % 2 == 0}

# From two lists
keys = ["name", "age", "city"]
values = ["Alice", 25, "Boston"]
person = {k: v for k, v in zip(keys, values)}

# String processing
text = "hello world"
char_count = {char: text.count(char) for char in set(text) if char != ' '}
```

**Nested Dictionaries**:
```python
# Nested dictionary for data analysis
sales_data = {
    "2023": {
        "Q1": {"revenue": 100000, "units": 500},
        "Q2": {"revenue": 120000, "units": 600},
        "Q3": {"revenue": 110000, "units": 550},
        "Q4": {"revenue": 130000, "units": 650}
    },
    "2024": {
        "Q1": {"revenue": 115000, "units": 575},
        "Q2": {"revenue": 135000, "units": 675}
    }
}

# Accessing nested data
print(sales_data["2023"]["Q1"]["revenue"])  # 100000

# Iterating through nested dictionary
for year, quarters in sales_data.items():
    print(f"Year: {year}")
    for quarter, data in quarters.items():
        print(f"  {quarter}: Revenue ${data['revenue']}, Units {data['units']}")
```

### Sets

**Set Operations**:
```python
# Creating sets
fruits = {"apple", "banana", "cherry"}
numbers = set([1, 2, 3, 4, 5])
empty_set = set()  # Note: {} creates empty dict, not set

# Set operations
fruits.add("orange")
fruits.update(["grape", "kiwi"])
fruits.remove("banana")  # Raises KeyError if not found
fruits.discard("mango")  # No error if not found

# Set mathematics
set1 = {1, 2, 3, 4, 5}
set2 = {4, 5, 6, 7, 8}

union = set1 | set2              # {1, 2, 3, 4, 5, 6, 7, 8}
intersection = set1 & set2       # {4, 5}
difference = set1 - set2         # {1, 2, 3}
symmetric_diff = set1 ^ set2     # {1, 2, 3, 6, 7, 8}

# Set methods
print(set1.union(set2))
print(set1.intersection(set2))
print(set1.difference(set2))
print(set1.symmetric_difference(set2))

# Set relationships
print(set1.issubset(set2))       # False
print(set1.issuperset({1, 2}))   # True
print(set1.isdisjoint({9, 10}))  # True
```

**Set Comprehensions**:
```python
# Basic set comprehension
squares = {x**2 for x in range(10)}

# With condition
even_squares = {x**2 for x in range(10) if x % 2 == 0}

# Removing duplicates from list
numbers = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4]
unique_numbers = list(set(numbers))

# Finding unique words
text = "the quick brown fox jumps over the lazy dog"
unique_words = {word.lower() for word in text.split()}
```

---

## 5. Regular Expressions (Regex) {#regex}

### Basic Regex Patterns

**Common Patterns**:
```python
import re

# Basic patterns
text = "The price is $25.99 and the discount is 15%"

# Find all digits
digits = re.findall(r'\d+', text)          # ['25', '99', '15']
print(digits)

# Find all floating point numbers
floats = re.findall(r'\d+\.\d+', text)     # ['25.99']
print(floats)

# Find words
words = re.findall(r'\w+', text)           # ['The', 'price', 'is', '25', '99', 'and', ...]
print(words)

# Find email pattern
email_text = "Contact us at info@company.com or support@help.org"
emails = re.findall(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', email_text)
print(emails)  # ['info@company.com', 'support@help.org']
```

**Regex Methods**:
```python
import re

text = "Python is great for data analysis. Python rocks!"

# re.search() - finds first match
match = re.search(r'Python', text)
if match:
    print(f"Found '{match.group()}' at position {match.start()}-{match.end()}")

# re.findall() - finds all matches
all_matches = re.findall(r'Python', text)
print(f"Found {len(all_matches)} occurrences")

# re.finditer() - returns iterator of match objects
for match in re.finditer(r'Python', text):
    print(f"Match: {match.group()} at {match.start()}-{match.end()}")

# re.sub() - substitute/replace
new_text = re.sub(r'Python', 'R', text)
print(new_text)  # "R is great for data analysis. R rocks!"

# re.split() - split string
parts = re.split(r'[.!]', text)
print(parts)  # ['Python is great for data analysis', ' Python rocks', '']
```

**Advanced Regex Patterns**:
```python
import re

# Groups and capturing
phone_text = "Call me at (555) 123-4567 or 555.987.6543"
phone_pattern = r'(\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4})'
phones = re.findall(phone_pattern, phone_text)
print(phones)  # ['(555) 123-4567', '555.987.6543']

# Named groups
date_text = "Today is 2024-03-15 and tomorrow is 2024-03-16"
date_pattern = r'(?P<year>\d{4})-(?P<month>\d{2})-(?P<day>\d{2})'
for match in re.finditer(date_pattern, date_text):
    print(f"Year: {match.group('year')}, Month: {match.group('month')}, Day: {match.group('day')}")

# Lookahead and lookbehind
password_text = "password123 mypass456 secure789pass"
# Password with at least one digit (positive lookahead)
strong_passwords = re.findall(r'\b\w*(?=\w*\d)\w*\b', password_text)
print(strong_passwords)  # ['password123', 'mypass456', 'secure789pass']
```

**Data Cleaning with Regex**:
```python
import re

# Clean messy data
messy_data = [
    "  John Doe  ",
    "jane.smith@email.com",
    "Phone: (555) 123-4567",
    "Age: 25 years old",
    "Salary: $50,000.00",
    "Invalid: N/A"
]

# Extract names (assuming first entry is name)
name_pattern = r'^[A-Za-z\s]+$'
names = [re.sub(r'\s+', ' ', item.strip()) for item in messy_data 
         if re.match(name_pattern, item.strip())]
print("Names:", names)

# Extract emails
email_pattern = r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'
emails = []
for item in messy_data:
    found_emails = re.findall(email_pattern, item)
    emails.extend(found_emails)
print("Emails:", emails)

# Extract phone numbers
phone_pattern = r'\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}'
phones = []
for item in messy_data:
    found_phones = re.findall(phone_pattern, item)
    phones.extend(found_phones)
print("Phones:", phones)

# Extract numeric values
numeric_pattern = r'\d+(?:,\d{3})*(?:\.\d{2})?'
numbers = []
for item in messy_data:
    found_numbers = re.findall(numeric_pattern, item)
    numbers.extend(found_numbers)
print("Numbers:", numbers)
```

**Text Processing for Analytics**:
```python
import re

# Log file analysis
log_data = """
2024-03-15 10:30:15 INFO User login successful: user123
2024-03-15 10:31:22 ERROR Database connection failed
2024-03-15 10:32:45 INFO User logout: user123
2024-03-15 10:33:10 WARNING High memory usage detected
2024-03-15 10:34:55 ERROR File not found: data.csv
"""

# Extract log entries
log_pattern = r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) (\w+) (.+)'
log_entries = re.findall(log_pattern, log_data.strip())

# Process log entries
for timestamp, level, message in log_entries:
    print(f"Time: {timestamp}, Level: {level}, Message: {message}")

# Count log levels
from collections import Counter
log_levels = [entry[1] for entry in log_entries]
level_counts = Counter(log_levels)
print("\nLog level counts:", dict(level_counts))

# Extract user activities
user_pattern = r'user(\w+)'
users = re.findall(user_pattern, log_data)
print("Users mentioned:", list(set(users)))
```

**Web Scraping Patterns**:
```python
import re

# HTML content simulation
html_content = """
<div class="product">
    <h2>Laptop Computer</h2>
    <p class="price">$999.99</p>
    <p class="description">High-performance laptop for data analysis</p>
</div>
<div class="product">
    <h2>External Monitor</h2>
    <p class="price">$299.99</p>
    <p class="description">27-inch 4K display</p>
</div>
"""

# Extract product names
product_names = re.findall(r'<h2>(.*?)</h2>', html_content)
print("Products:", product_names)

# Extract prices
prices = re.findall(r'class="price">\$(\d+\.\d{2})</p>', html_content)
print("Prices:", prices)

# Extract descriptions
descriptions = re.findall(r'class="description">(.*?)</p>', html_content)
print("Descriptions:", descriptions)

# Create structured data
products = []
for i in range(len(product_names)):
    product = {
        'name': product_names[i],
        'price': float(prices[i]),
        'description': descriptions[i]
    }
    products.append(product)

print("\nStructured data:")
for product in products:
    print(product)
```

---

*Continue to Part 2 for Slicing, DataFrames, File Management, Classes and Functions*