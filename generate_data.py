import random

def generate_data():
    with open('database.txt', 'w') as f:
        for i in range(100000):
            f.write(str(i) + ' ' + str(random.randint(1, 100000)) + '\n')
    
if __name__ == '__main__':
    generate_data()