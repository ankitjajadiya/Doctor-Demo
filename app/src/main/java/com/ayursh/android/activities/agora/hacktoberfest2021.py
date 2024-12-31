def count_substring(string, subs):
    count = 0
    k=len(subs)
    for i in range(0, len(string)-k+1):
        if string[i:i+k]==subs:
            count+=1
    return count

if __name__ == '__main__':
    string = input()
    sub_string = input()
    
    count = count_substring(string, sub_string)
    print(count)