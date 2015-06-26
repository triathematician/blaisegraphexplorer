See also GraphFileFormats.

# _Edge List_ file format #
The simplest file format supported by GraphExplorer consists of a pair of integers (representing an arc) on each line. For example:
```
1 5
2 3
1 4
3 4
4 5
```
The integers may also be separated by any combination of commas and spaces:
```
1, 5
2, 3
1  ,4
3, 4
4 , 5
```
Anything after the second integer is ignored. The returned result is a directed graph, whose vertices correspond to the integers in the file.

Typically, edge lists will be stored in a simple text file, often with a `.txt` extension.