import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class BulkUpdateTestWithInit {
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

            // 测试 Bulk Update
            long updateTime = bulkUpdateTest(conn, totalRows, batchSize);

            // 输出耗时
            System.out.println("Bulk Update 用时: " + updateTime / 1000.0 + " 秒");

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

    // 执行 Bulk Update 测试
    private static long bulkUpdateTest(Connection conn, int totalRows, int batchSize) throws Exception {
        String updateSQL = "UPDATE bulk_test SET data = ? WHERE category = ?";
        long startTime = System.currentTimeMillis();

        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows / batchSize; i++) {
                pstmt.setString(1, "updated_data");
                pstmt.setInt(2, i % 10); // 假设按 category 更新
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