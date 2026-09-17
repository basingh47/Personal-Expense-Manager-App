def format_indian(int_part):
    if len(int_part) <= 3:
        return int_part
    last_3 = int_part[-3:]
    remaining = int_part[:-3]
    parts = []
    while len(remaining) > 2:
        parts.insert(0, remaining[-2:])
        remaining = remaining[:-2]
    if remaining:
        parts.insert(0, remaining)
    return ",".join(parts) + "," + last_3

def format_international(int_part):
    if len(int_part) <= 3:
        return int_part
    parts = []
    rem = int_part
    while len(rem) > 3:
        parts.insert(0, rem[-3:])
        rem = rem[:-3]
    if rem:
        parts.insert(0, rem)
    return ",".join(parts)

for val in ["5", "50", "500", "5000", "50000", "500000", "5000000", "50000000"]:
    print(f"{val} -> IN: {format_indian(val)} | US: {format_international(val)}")
