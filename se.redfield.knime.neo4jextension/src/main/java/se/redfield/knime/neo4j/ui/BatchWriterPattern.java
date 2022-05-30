package se.redfield.knime.neo4j.ui;

public enum BatchWriterPattern implements BatchPattern {
    ALL_COLUMN(
            "Simple create",
            "Create nodes with all input columns as properties",
            "UNWIND $batch as row\n" +
                    "CREATE (n:Label)\n" +
                    "SET n += row\n" +
                    "RETURN id(n)"
    ),
    SELECTED_COLUMN(
            "Simple create with selected columns",
            "Create nodes with selected columns as properties",
            "UNWIND $batch as row\n" +
                    "CREATE (n:Label)\n" +
                    "SET n.property_name = row.column_name\n" +
                    "RETURN id(n), n.property_name"
    ),
    SIMPLE_SELECTED_COLUMN(
            "Create with selected columns",
            "Create nodes with selected columns as properties",
            "UNWIND $batch as row\n" +
                    "MERGE (n:Label {id: row.id})\n" +
                    "(ON CREATE) SET n += row.column_name"
    ),
    UPDATE_EXISTING_NODES(
            "Update of existing nodes",
            "Update of existing nodes by id",
            "WITH $batch as data, [k in keys($batch) | toInteger(k)] as ids\n" +
                    "MATCH (n) WHERE id(n) IN ids\n" +
                    "// single property value\n" +
                    "SET n.count = data[toString(id(n))]\n" +
                    "// or override all properties\n" +
                    "SET n = data[toString(id(n))]\n" +
                    "// or add all properties\n" +
                    "SET n += data[toString(id(n))]"
    ),
    MATCH_NODES_AND_CREATE_RELATIONSHIP(
            "Match nodes and create relationship",
            "Match nodes and create relationship with property between them",
            "UNWIND $batch as row\n" +
                    "MATCH (n1:Label1) WHERE n1.property_name1 = row.column_name1\n" +
                    "MATCH (n2:Label2) WHERE n2.property_name2 = row.column_name2\n" +
                    "CREATE (n1)-[rel:RELATIONSHIP_LABEL]->(n2) \n" +
                    "SET rel.property_name3 = row.column_name3\n" +
                    "RETURN id(rel), row.property_name3, n1.property_name1, n2.property_name2"
    ),
    UPDATE_EXISTING_RELATIONSHIP(
            "Update of existing relationships",
            "Update of existing relationships by id",
            "WITH $batch as data, [k in keys($batch) | toInteger(k)] as ids\n" +
                    "MATCH ()-[rel]->() WHERE id(rel) IN ids\n" +
                    "SET rel.property_name = data[toString(id(rel))] // single property\n" +
                    "SET rel = data[toString(id(rel))] // all properties"
    );

    private String name;
    private String description;
    private String script;

    BatchWriterPattern(String name, String description, String script) {
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
