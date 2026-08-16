package io.edgemania.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Node {
    private final String id;
    private String typeId;
    private String label;
    private String category;
    private double x;
    private double y;
    private final Map<String, Object> properties;
    private String status;

    public Node(String typeId, String label, String category, double x, double y,
                Map<String, Object> properties) {
        this.id = UUID.randomUUID().toString();
        this.typeId = typeId;
        this.label = label;
        this.category = category;
        this.x = x;
        this.y = y;
        this.properties = new HashMap<>(properties != null ? properties : Map.of());
        this.status = "idle";
    }

    public String getId() { return id; }
    public String getTypeId() { return typeId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCategory() { return category; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> props) { this.properties.clear(); this.properties.putAll(props); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
