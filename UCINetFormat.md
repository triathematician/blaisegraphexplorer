See also GraphFileFormats.

# _UCINet_ file format #
See also http://www.analytictech.com/networks/dataentry.htm.

The _UCINET_ format consists of a _header_ section and a _data_ section. A sample header section is:
```
DL
N=17 NM=1
format = fullmatrix diagonal present
data:
...
```
The `DL` line declares the start of the header. The `N=17` sets the number of vertices. The `NM=` declares the number of expected matrices. The `format` argument specifies the format of the data to follow (in this case, a full 17x17 adjacency matrix). The `data:` line declares the start of graph data. Comments:
  * May be placed in a single line: `DL N=17 NM=1`.
  * The number of matrices is assumed to be 1 if not specified.
  * The default format is (I believe) `fullmatrix`.

Optionally, labels for the vertices may be declared before the data is specified (labels cannot contain spaces or commas, and may be on separate lines). In this case, a labels section is added before the `data:` section:
```
...
format = ...
labels:
a, b, c, d
e f g
data:
...
```

Alternately, the labels may be embedded within the data, requiring a `labels embedded` command:
```
...
format = ...
labels embedded
...
```

## Data Formats ##

The currently supported data formats are `fullmatrix`, `edgelist`, and `nodelist1`.