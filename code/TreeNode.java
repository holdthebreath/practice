package code;

import java.util.*;
import java.util.stream.Collectors;

public class TreeNode {
    Integer val;
    TreeNode left;
    TreeNode right;

    public TreeNode(Integer value, TreeNode left, TreeNode right) {
        this.val = value;
        this.left = left;
        this.right = right;
    }

    public static List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }


    public static List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Deque<TreeNode> stack = new LinkedList<>();
        if (root == null)
            return result;
        TreeNode index;
        if (root.left != null) {
            stack.offerLast(root.left);
        }
        while (stack.size() > 0) {
            index = stack.pollLast();
            if (index.left == null) {
                result.add(index.val);
                if (index.right != null)
                    result.add(index.right.val);
                break;
            }
            if (index.right != null) {
                stack.offerLast(index.right);
            }
            stack.offerLast(index);
            if (index.left != null) {
                stack.offerLast(index.left);
            }
        }
        int size = stack.size();
        for (int i = 0; i < size; i++) {
            result.add(stack.pollLast().val);
        }
        result.add(root.val);
        if (root.right != null) {
            stack.offerLast(root.right);
        }
        while (stack.size() > 0) {
            index = stack.pollLast();
            if (index.left == null) {
                result.add(index.val);
                if (index.right != null)
                    result.add(index.right.val);
                break;
            }
            if (index.right != null) {
                stack.offerLast(index.right);
            }
            stack.offerLast(index);
            if (index.left != null) {
                stack.offerLast(index.left);
            }
        }
        size = stack.size();
        for (int i = 0; i < size; i++) {
            result.add(stack.pollLast().val);
        }
        return result;
    }


    public static List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    public static void preorder(TreeNode node, List<Integer> result) {
        result.add(node.val);
        if (node.left != null)
            preorder(node.left, result);
        if (node.right != null)
            preorder(node.right, result);
    }

    public static void inorder(TreeNode node, List<Integer> result) {
        if (node.left != null)
            inorder(node.left, result);
        result.add(node.val);
        if (node.right != null)
            inorder(node.right, result);
    }

    public static void postorder(TreeNode node, List<Integer> result) {
        if (node.left != null)
            postorder(node.left, result);
        if (node.right != null)
            postorder(node.right, result);
        result.add(node.val);
    }

//    public static TreeNode buildTree(int[] inorder, int[] postorder) {
//        return buildNode(inorder, postorder, 0, postorder.length);
//    }

    static int index;

    public static TreeNode buildTree(int[] preorder, int[] inorder) {
        if (preorder.length == 0 || inorder.length == 0)
            return null;
        index = 0;
        return doBuildTree(preorder, inorder, index, inorder.length - 1);
    }

    public static TreeNode doBuildTree(int[] preorder, int[] inorder, int start, int end) {
        if (start > end)
            return null;
        TreeNode node = new TreeNode(preorder[index++], null, null);
        if (start == end)
            return node;
        int midIndex = 0;
        for (int i = 0; i < end; i++) {
            if (inorder[i] == node.val) {
                midIndex = i;

                break;
            }
        }
        node.left = doBuildTree(preorder, inorder, start, midIndex - 1);
        node.right = doBuildTree(preorder, inorder, midIndex + 1, end);
        return node;
    }

    public static int[] postorder() {
        List<String> result = new ArrayList<>();
        String a = result.stream().map(String::valueOf).collect(Collectors.joining());
        System.out.println(a);
        Integer number = Integer.valueOf(a.substring(0, 1)) - 2;


        return result.stream().mapToInt(Integer::valueOf).toArray();
    }

    public static void main(String[] args) {
        int[][] b = new int[][]{{10, 16}, {2, 8}, {1, 6}, {7, 12}};
        Arrays.sort(b);

        List<int[]> r = new ArrayList<>();

        int a = 1;
        String d = a + "";
//        char[] index = d.to();
//        Integer.valueOf(Arrays.toString(index));
        System.out.println(b);
    }

}
