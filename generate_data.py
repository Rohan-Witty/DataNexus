import random

def generate_data():
    with open('database.txt', 'w') as f:
        for i in range(100000):
            f.write(str(i) + ' ' + str(random.randint(1, 100000)) + '\n')

def generate_tests():
    with open('testcase.txt', 'w') as f:
        #  Allowed commands are put, get, del, store, exit
        commands = ['put', 'get', 'del', 'store']
        for i in range(1000):
            command = random.choice(commands)
            if command == 'put':
                f.write(command + ' ' + str(random.randint(1, 100000)) + ' ' + str(random.randint(1, 100000)) + '\n')
            elif command == 'get' or command == 'del':
                f.write(command + ' ' + str(random.randint(1, 100000)) + '\n')
            else:
                f.write(command + '\n')
        f.write('exit\n')


if __name__ == '__main__':
    generate_tests()