package org.example;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

public class Main {
    private static final String URL = "jdbc:postgresql://localhost:5432/Strings";
    private static final String USER = ""; //Впишите имя сервера
    private static final String PASSWORD = ""; //Впишите пароль от сервера
    private static String currentTable = ""; //Оставьте пустую строку

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to PostgreSQL database");

            boolean isRunning = true;
            while (isRunning) {
                printMenu();
                int input = sc.nextInt();
                sc.nextLine();

                switch (input) {
                    case 1:
                        showAllTables(conn);
                        break;
                    case 2:
                        createTable(conn, sc);
                        break;
                    case 3:
                        insertStrings(conn, sc);
                        break;
                    case 4:
                        calculateStringLengths(conn);
                        break;
                    case 5:
                        concatenateStrings(conn);
                        break;
                    case 6:
                        compareStrings(conn);
                        break;
                    case 7:
                        exportToExcel(conn);
                        break;
                    case 0:
                        isRunning = false;
                        System.out.println("Выход из программы");
                        break;
                    default:
                        System.out.println("Неверный выбор");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка базы данных:");
            e.printStackTrace();
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Главное меню ---");
        System.out.println("1. Вывести все таблицы из SQL");
        System.out.println("2. Создать таблицу в SQL");
        System.out.println("3. Ввести две строки с клавиатуры, результат сохранить в SQL с последующим выводом в консоль");
        System.out.println("4. Подсчитать размер ранее введенных строк, результат сохранить в SQL с последующим выводом в консоль");
        System.out.println("5. Объединить две строки в единое целое, результат сохранить в SQL с последующим выводом в консоль");
        System.out.println("6. Сравнить две ранее введенные строки, результат сохранить в SQL с последующим выводом в консоль");
        System.out.println("7. Сохранить все данные (вышеполученные результаты) из SQL в Excel и вывести на экран");
        System.out.println("0. Выход");
        System.out.println("Выберите пункт: ");
    }

    private static void showAllTables(Connection conn) throws SQLException {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean hasTables = false;
            System.out.println("\nСписок таблиц:");
            while (rs.next()) {
                hasTables = true;
                System.out.println("- " + rs.getString("table_name"));
            }
            if (!hasTables) {
                System.out.println("\nТаблиц пока нет");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTable(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Введите имя таблицы: ");
        String tableName = sc.nextLine();

        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id SERIAL PRIMARY KEY, " +
                "string1 TEXT, " +
                "string2 TEXT, " +
                "length1 INTEGER, " +
                "length2 INTEGER, " +
                "concatenated TEXT, " +
                "comparison_result TEXT)", tableName);

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            currentTable = tableName;
            System.out.println("\nТаблица '" + tableName + "' создана");
        }
    }

    private static boolean isTableSelected() {
        if (currentTable == null || currentTable.trim().isEmpty()) {
            System.out.println("Сначала выберите или создайте таблицу!");
            return false;
        }
        return true;
    }

    private static void insertStrings(Connection conn, Scanner sc) throws SQLException {
        if (!isTableSelected()) return;
        try {
            System.out.print("\nВведите первую строку: ");
            String str1 = sc.nextLine();
            if (str1.isEmpty()) {
                System.out.println("Отменено");
                return;
            }

            System.out.print("\nВведите вторую строку: ");
            String str2 = sc.nextLine();
            if (str2.isEmpty()) {
                System.out.println("Отменено");
                return;
            }

            String sql = "INSERT INTO " + currentTable + " (string1, string2) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, str1);
                pstmt.setString(2, str2);
                pstmt.executeUpdate();
                System.out.println("Строки успешно сохранены");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при сохранении строк: " + e.getMessage());
        }
    }

    private static void calculateStringLengths(Connection conn) throws SQLException {
        if (!isTableSelected()) return;
        String updateSql = "UPDATE " + currentTable + " SET length1 = LENGTH(string1), length2 = LENGTH(string2) "
                + "WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        String selectSql = "SELECT length1, length2 FROM " + currentTable + " WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(updateSql);
            System.out.println("Длины строк подсчитаны для " + rows + " записи(ей)");
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                if (rs.next()) {
                    System.out.println("Длина строки 1: " + rs.getInt("length1"));
                    System.out.println("Длина строки 2: " + rs.getInt("length2"));
                }
            }
        }
    }

    private static void concatenateStrings(Connection conn) throws SQLException {
        if (!isTableSelected()) return;
        String sql = "UPDATE " + currentTable + " SET concatenated = string1 || string2 " +
                "WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        String selectSql = "SELECT concatenated FROM " + currentTable + " WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println("Строки объединены для " + rows + " записи");
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                if (rs.next()) {
                    System.out.println("Результат объединения: " + rs.getString("concatenated"));
                }
            }
        }
    }

    private static void compareStrings(Connection conn) throws SQLException {
        if (!isTableSelected()) return;
        String sql = "UPDATE " + currentTable + " SET comparison_result = CASE " +
                "WHEN string1 = string2 THEN 'Строки равны' " +
                "ELSE 'Строки разные' END " +
                "WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        String selectSql = "SELECT comparison_result FROM " + currentTable + " WHERE id = (SELECT MAX(id) FROM " + currentTable + ")";
        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println("Строки сравнены для " + rows + " записи");
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                if (rs.next()) {
                    System.out.println("Результат сравнения: " + rs.getString("comparison_result"));
                }
            }
        }
    }

    private static void exportToExcel(Connection conn) {
        if (!isTableSelected()) return;
        String sql = "SELECT id, string1, string2, length1, length2, concatenated, comparison_result FROM " + currentTable;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Strings");
            Row header = sheet.createRow(0);
            String[] columns = {"id", "string1", "string2", "length1", "length2", "concatenated", "comparison_result"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.length; i++) {
                    row.createCell(i).setCellValue(rs.getString(i + 1));
                }
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            String fileName = currentTable + ".xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }
            System.out.println("Данные успешно экспортированы в файл: " + fileName);
        } catch (SQLException e) {
            System.out.println("Ошибка при получении данных из БД: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Ошибка при записи Excel-файла: " + e.getMessage());
        }
    }
}