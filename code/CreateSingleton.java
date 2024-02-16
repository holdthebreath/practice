package code;

import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CreateSingleton {
    private static volatile CreateSingleton singleton;

    public CreateSingleton getSingleton() {
        if (singleton == null) {
            synchronized (CreateSingleton.class) {
                if (singleton == null)
                    singleton = new CreateSingleton();
            }
        }
        return singleton;
    }


    public static void main(String[] args) {
//        TreeNode node1 = new TreeNode(3, null, null);
//        TreeNode node2 = new TreeNode(4, null, null);
//        TreeNode node3 = new TreeNode(2, node1, node2);
//        TreeNode node4 = new TreeNode(3, null, null);
//        TreeNode node5 = new TreeNode(4, null, null);
//        TreeNode node6 = new TreeNode(2, node4, node5);
//        TreeNode root = new TreeNode(5, node3, node6);
//        List<Integer> preorderTraversal = TreeNode.preorderTraversal(root);
//        for (int i = 0; i < preorderTraversal.size(); i++) {
//            System.out.print(preorderTraversal.get(i));
//        }
//        System.out.println();
//        List<Integer> inorderTraversal = TreeNode.inorderTraversal(root);
//        for (int i = 0; i < inorderTraversal.size(); i++) {
//            System.out.print(inorderTraversal.get(i));
//        }
//        System.out.println();
//        List<Integer> postorderTraversal = TreeNode.postorderTraversal(root);
//        for (int i = 0; i < postorderTraversal.size(); i++) {
//            System.out.print(postorderTraversal.get(i));
//        }

//        int[] inorder = new int[]{9,3,15,20,7};
//        int[] postorder = new int[]{9,15,7,20,3};

//        int[] perorder = new int[]{1,2};
//        int[] inorder = new int[]{2,1};

    List<String> c = new ArrayList<>();
        String collect = c.stream().collect(Collectors.joining("."));
        String a  =   String.valueOf((char)97);
    boolean[][] b = new boolean[a.length()][a.length()];
        System.out.println(b);
        System.out.println(a);

        String[][] d = new String[1][1];
        Arrays.fill(d, "");
//        System.out.println(TreeNode.buildTree(inorder, perorder));
    }
}
