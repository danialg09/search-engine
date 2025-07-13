package searchengine.indexing;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class LinkNode {

    private String url;

    private int depth;

    private List<LinkNode> children;

    public LinkNode(String url, int depth) {

        this.url = url;

        this.depth = depth;

        this.children = new ArrayList<>();

    }

    public void addChild(LinkNode child) {

        this.children.add(child);

    }
}

