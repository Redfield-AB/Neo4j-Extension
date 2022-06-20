package se.redfield.knime.neo4j.ui;

public enum BatchReaderPattern implements BatchPattern {
    MATCH_NODES(
            "Match nodes",
            "Match nodes by property",
            "UNWIND $batch as row\n" +
                    "MATCH (n:Label) \n" +
                    "WHERE n.property = row.property\n" +
                    "RETURN id(n)"
    ),
    MATCH_RELATIONSHIPS(
            "Match relationships",
            "Match relationships by node property",
            "UNWIND $batch as row\n" +
                    "MATCH (n:Label)-[rel:RELATIONSHIP]->(m)\n" +
                    "WHERE n.property = row.property\n" +
                    "RETURN id(rel)"
    ),
    MATCH_NEIGHBOUR(
            "Match neighbour nodes and aggregate",
            "Match neighbour nodes and aggregate the result",
            "UNWIND $batch as row\n" +
                    "MATCH (n:Label)-[rel:RELATIONSHIP]->(m)\n" +
                    "WHERE n.property1 = row.property1\n" +
                    "RETURN id(n), collect(m.property2)"
    ),
    COMLEX_SEARCH_QUERY(
            "Comlex search query",
            "Comlex search query",
            "WITH $batch as data, [k in keys($batch) | toInteger(k)] as ids\n" +
                    "MATCH (n)-[rel]->(m)\n" +
                    "WHERE id(rel) IN ids\n" +
                    "RETURN id(n), rel.property1, m.property2 "
    );

    private String name;
    private String description;
    private String script;

    BatchReaderPattern(String name, String description, String script) {
        this.name = name;
        this.description = description;
        this.script = script;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getScript() {
        return script;
    }
    public void setScript(String script) {
        this.script = script;
    }
}
