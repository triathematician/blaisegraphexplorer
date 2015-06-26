Dynamic graphs are graphs that change over time.

# Loading and Creating Dynamic Graphs #
Most graph file formats do not support dynamic graphs. The _pajek_ format has limited support for dynamic graphs. See GraphFileFormats for a description of the syntax.

To generate a dynamic graph, select `File >> Create >> Preferential-attachment random graph`, and choose the `longitudinal graph` result type. This provides a quick way to explore the dynamic graph features within GraphExplorer

## Views of Dynamic Graphs ##
When dynamic graphs are loaded, GraphExplorer displays a timeline in addition to the main graph view. If the layout is animating, the main view will automatically update as the time is adjusted. This provides the quickest way to get a sense for how the graph is changing over time.

## Metric Computations on Dynamic Graphs ##
Other views, such as the computation of a metric, are also adjusted when the dynamic graph timeline is adjusted. The **Longitudinal Metric Chart** tab displays the change in metrics over time. To activate the chart, first select a subset of nodes from `View >> Highlight a subset of nodes`.

# Export Options #
Dynamic graph layouts can be exported as a movie. Select "`File >> Export Movie...`".