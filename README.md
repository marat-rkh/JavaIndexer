JavaIndexer
===========

Для сборки необходимы maven и java 7 (протестированно на Ubuntu 14.04 и MacOsX Mavericks).
Чтобы скомпилировать проект, перейдите в папку с проектом и выполните 'mvn compile'.
Для запуска тестов используйте 'mvn test'. Один из тестов потребует довольно много времени (около 40 секунд). Он тестирует асинхронные события.

Чтобы запустить пример, демонстрирующий работу библиотеки, выполните 'mvn exec:java'. Программа предложит вам выполнить нужные команды для работы с диском, а именно:  

Commands:  

a <file_or_dir_path> - add file or dir to index

r <file_or_dir_path> - remove file or dir from index

s <word>             - get list of files containing <word>

c <file_path>        - check if index contains file

h                    - show this help

q                    - finish work

Кроме того, для автоматизации тестирования примера, вы можете использовать 'mvn exec:java -Dexec.args="/path/to/commands/file"', где в качестве аргумента указать путь к файлу с перечисленными в нём командами в формате, представленном выше. Пример файла:

a /home/mrx/Test

a /home/mrx/Test2

s my

s die

s себя

c /home/mrx/Test/darkValleys.txt

r /home/mrx/Test2

c /home/mrx/Test/darkValleys.txt

s my

s die

s себя

q

Все команды из файла будут выполнены по очереди.
