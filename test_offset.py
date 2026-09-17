def build_formatted_and_mapping(raw_text, is_indian=True):
    if not raw_text:
        return "", [0], [0]
    
    # Split into integer and decimal parts
    parts = raw_text.split(".", 1)
    int_part = parts[0]
    has_dec = len(parts) > 1
    dec_part = parts[1] if has_dec else ""
    
    # Format integer part
    if not int_part:
        formatted_int = ""
        # Commas mapping
        # int_part is empty, e.g. ".50"
    else:
        # Build formatted integer part
        if is_indian:
            if len(int_part) <= 3:
                formatted_int = int_part
            else:
                last_3 = int_part[-3:]
                rem = int_part[:-3]
                chunks = []
                while len(rem) > 2:
                    chunks.insert(0, rem[-2:])
                    rem = rem[:-2]
                if rem:
                    chunks.insert(0, rem)
                formatted_int = ",".join(chunks) + "," + last_3
        else:
            if len(int_part) <= 3:
                formatted_int = int_part
            else:
                chunks = []
                rem = int_part
                while len(rem) > 3:
                    chunks.insert(0, rem[-3:])
                    rem = rem[:-3]
                if rem:
                    chunks.insert(0, rem)
                formatted_int = ",".join(chunks)
    
    if has_dec:
        formatted_text = formatted_int + "." + dec_part
    else:
        formatted_text = formatted_int
        
    # Build exact offset mappings
    # Map each index of raw_text to formatted_text
    orig_to_trans = [0] * (len(raw_text) + 1)
    trans_to_orig = [0] * (len(formatted_text) + 1)
    
    # Walk through formatted_text
    orig_idx = 0
    for trans_idx, ch in enumerate(formatted_text):
        trans_to_orig[trans_idx] = orig_idx
        if ch != ",":
            orig_to_trans[orig_idx] = trans_idx
            orig_idx += 1
    
    orig_to_trans[len(raw_text)] = len(formatted_text)
    trans_to_orig[len(formatted_text)] = len(raw_text)
    
    return formatted_text, orig_to_trans, trans_to_orig

for s in ["5", "50", "500", "5000", "50000", "500000", "50000.5", "50000.50", ""]:
    f_text, o2t, t2o = build_formatted_and_mapping(s, True)
    print(f"Raw: '{s}' -> Formatted: '{f_text}'")
    print(f"  o2t: {o2t}")
    print(f"  t2o: {t2o}")
    # Verify bounds
    for i in range(len(s) + 1):
        target = o2t[i]
        assert 0 <= target <= len(f_text), f"o2t out of bounds for {i}: {target}"
    for j in range(len(f_text) + 1):
        target = t2o[j]
        assert 0 <= target <= len(s), f"t2o out of bounds for {j}: {target}"

print("All assertion tests passed!")
