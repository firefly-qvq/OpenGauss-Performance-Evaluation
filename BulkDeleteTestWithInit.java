import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class BulkDeleteTestWithInit {
    public static void main(String[] args) {
        // 数据库连接配置
        String dbName = "postgres";
        String dbUser = "gaussdb";
        String dbPassword = "Secretpassword@123";
        String dbHost = "localhost";
        String dbPort = "15433";
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        // 测试数据量和批量大小
        int totalRows = 1000000;
        int batchSize = 10000;

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            // 初始化表格
            initializeTable(conn, totalRows);

            // 测试 Bulk Delete
            long deleteTime = bulkDeleteTest(conn, totalRows, batchSize);

            // 输出耗时
            System.out.println("Bulk Delete 用时: " + deleteTime / 1000.0 + " 秒");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 初始化测试表格
    private static void initializeTable(Connection conn, int totalRows) throws Exception {
        String dropTableSQL = "DROP TABLE IF EXISTS bulk_test";
        String createTableSQL = "CREATE TABLE bulk_test (" +
                "id SERIAL PRIMARY KEY, " +
                "category INT, " +
                "data TEXT)";
        String insertSQL = "INSERT INTO bulk_test (category, data) VALUES (?, ?)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(dropTableSQL);
            stmt.executeUpdate(createTableSQL);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows; i++) {
                pstmt.setInt(1, i % 10); // 分类数据
                pstmt.setString(2, "random_data_" + i);
                pstmt.addBatch();

                if (i % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
        System.out.println("表格初始化完成，插入了 " + totalRows + " 行数据");
    }

    // 执行 Bulk Delete 测试
    private static long bulkDeleteTest(Connection conn, int totalRows, int batchSize) throws Exception {
        String deleteSQL = "DELETE FROM bulk_test WHERE category = ?";
        long startTime = System.currentTimeMillis();

        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows / batchSize; i++) {
                pstmt.setInt(1, i % 10); // 假设按 category 删除
                pstmt.addBatch();

                if (i % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}