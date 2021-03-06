package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.util.ColorConversion;

public class Classification
{
    public static final class ByRank implements Comparator<Classification>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Classification c1, Classification c2)
        {
            return c1.getRank() > c2.getRank() ? -1 : c1.getRank() < c2.getRank() ? 1 : 0;
        }
    }

    public static class Assignment
    {
        private InvestmentVehicle investmentVehicle;
        private int weight;
        private int rank;

        public Assignment()
        {
            // needed for xstream de-serialization
        }

        public Assignment(InvestmentVehicle account)
        {
            this(account, ONE_HUNDRED_PERCENT);
        }

        public Assignment(InvestmentVehicle investmentVehicle, int weight)
        {
            this.weight = weight;
            this.investmentVehicle = investmentVehicle;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public InvestmentVehicle getInvestmentVehicle()
        {
            return investmentVehicle;
        }

        public int getRank()
        {
            return rank;
        }

        public void setRank(int rank)
        {
            this.rank = rank;
        }
    }

    public static final int ONE_HUNDRED_PERCENT = 100 * Values.Weight.factor();

    public static final String UNASSIGNED_ID = "$unassigned$"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private String color;

    private Classification parent;
    private List<Classification> children = new ArrayList<Classification>();
    private List<Assignment> assignments = new ArrayList<Assignment>();

    private int weight;
    private int rank;

    public Classification()
    {
        // needed for xstream de-serialization
    }

    public Classification(String id, String name)
    {
        this(null, id, name);
    }

    public Classification(Classification parent, String id, String name, String color)
    {
        this.parent = parent;
        this.id = id;
        this.name = name;
        this.color = color;

        if (color == null)
        {
            Random r = new Random();
            this.color = '#' + Integer.toHexString(((r.nextInt(128) + 127) << 16) //
                            | ((r.nextInt(128) + 127) << 8) //
                            | (r.nextInt(128) + 127));
        }

        this.weight = ONE_HUNDRED_PERCENT;
    }

    public Classification(Classification parent, String id, String name)
    {
        this(parent, id, name, null);
    }

    public String getId()
    {
        return id;
    }

    /* package */void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public Classification getParent()
    {
        return parent;
    }

    public void setParent(Classification parent)
    {
        this.parent = parent;
    }

    public List<Classification> getChildren()
    {
        return children;
    }

    public void addChild(Classification classification)
    {
        children.add(classification);
    }

    public List<Assignment> getAssignments()
    {
        return assignments;
    }

    public void addAssignment(Assignment assignment)
    {
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment)
    {
        assignments.remove(assignment);
    }

    public int getWeight()
    {
        return weight;
    }

    public int getChildrenWeight()
    {
        int sum = 0;
        for (Classification child : children)
            sum += child.getWeight();
        return sum;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int rank)
    {
        this.rank = rank;
    }

    public String getPathName(boolean includeParent, int limit)
    {
        LinkedList<Classification> path = getPath();

        // remove root node
        if (!includeParent && path.size() > 1)
            path.removeFirst();

        // short circuit
        if (path.size() == 1)
            return path.get(0).getName();

        // add as many elements from left to right as possible

        int available = limit;

        StringBuilder leftBuffer = new StringBuilder();
        StringBuilder rightBuffer = new StringBuilder();

        int left = 0;
        int right = 0;

        while (left + right < path.size())
        {
            if ((left + right) % 2 == 0) // start right
            {
                // do right
                Classification c = path.get(path.size() - 1 - right);
                available -= c.getName().length();

                if (available < 0)
                    break;

                if (rightBuffer.length() > 0)
                    rightBuffer.insert(0, " » "); //$NON-NLS-1$
                rightBuffer.insert(0, c.getName());
                right++;
            }
            else
            {
                // do left
                Classification c = path.get(left);
                available -= c.getName().length();

                if (available < 0)
                    break;

                if (leftBuffer.length() > 0)
                    leftBuffer.append(" » "); //$NON-NLS-1$
                leftBuffer.append(c.getName());
                left++;
            }
        }

        if (left + right == path.size())
            return leftBuffer.toString() + " » " + rightBuffer.toString(); //$NON-NLS-1$
        else
            return leftBuffer.toString() + " ... " + rightBuffer.toString(); //$NON-NLS-1$
    }

    public String getPathName(boolean includeParent)
    {
        LinkedList<Classification> path = getPath();
        if (!includeParent && path.size() > 1)
            path.removeFirst();

        StringBuilder buf = new StringBuilder();

        for (Classification c : path)
        {
            if (buf.length() > 0)
                buf.append(" » "); //$NON-NLS-1$

            buf.append(c.getName());
        }

        return buf.toString();
    }

    private LinkedList<Classification> getPath()
    {
        LinkedList<Classification> path = new LinkedList<Classification>();

        Classification c = this;
        while (c != null)
        {
            path.addFirst(c);
            c = c.getParent();
        }

        return path;
    }

    public List<Classification> getTreeElements()
    {
        List<Classification> answer = new ArrayList<Classification>();

        LinkedList<Classification> stack = new LinkedList<Classification>();
        stack.addAll(getChildren());

        while (!stack.isEmpty())
        {
            Classification c = stack.pop();
            answer.add(c);
            stack.addAll(0, c.getChildren());
        }

        return answer;
    }

    public List<Classification> getPathToRoot()
    {
        LinkedList<Classification> path = new LinkedList<Classification>();

        Classification item = this;
        while (item != null)
        {
            path.addFirst(item);
            item = item.getParent();
        }

        return path;
    }

    public void assignRandomColors()
    {
        Random random = new Random();

        float hue = random.nextFloat() * 360f;
        float saturation = (random.nextFloat() * 0.5f) + 0.3f;
        float brightness = (random.nextFloat() * 0.4f) + 0.5f;

        assignRandomColors(hue, saturation, brightness);
    }

    /* package */void assignRandomColors(float hue, float saturation, float brightness)
    {
        if (children.isEmpty())
            return;

        Collections.sort(children, new ByRank());

        int size = children.size();
        float step = 360f / (float) size;

        int index = 0;
        for (Classification child : children)
        {
            float h = (hue + (step * index)) % 360f;

            child.setColor(ColorConversion.toHex(h, saturation, brightness));
            child.cascadeColorDown(h, saturation, brightness);
            index++;
        }
    }

    public void cascadeColorDown()
    {
        if (children.isEmpty())
            return;

        float[] hsb = ColorConversion.toHSB(color);
        cascadeColorDown(hsb[0], hsb[1], hsb[2]);
    }

    private void cascadeColorDown(float hue, float saturation, float brightness)
    {
        if (children.isEmpty())
            return;

        float childSaturation = Math.max(0f, saturation - 0.1f);
        float childBrightness = Math.min(1f, brightness + 0.1f);

        for (Classification child : children)
        {
            child.setColor(ColorConversion.toHex(hue, childSaturation, childBrightness));
            child.cascadeColorDown(hue, childSaturation, childBrightness);
        }
    }

    public void accept(Visitor visitor)
    {
        visitor.visit(this);

        for (Classification child : new ArrayList<Classification>(children))
            child.accept(visitor);

        for (Assignment assignment : new ArrayList<Assignment>(assignments))
            visitor.visit(this, assignment);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
