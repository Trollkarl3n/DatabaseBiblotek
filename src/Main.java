import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    private static int loggedInUserId = -1;

    public static void main(String[] args) {
        bibloteksuppg();
    }

    private static void bibloteksuppg() {
        // Inställningar för att ansluta till databasen
        String username = (System.getenv("DBUSER") != null ? System.getenv("DBUSER") : "root");
        String password = (System.getenv("DBPASS") != null ? System.getenv("DBPASS") : "password");

        Database.username = username;
        Database.password = password;
        Database.port = 3306;
        Database.database = "BiblotekUppg";

        Connection connection = Database.getConnection();
        if (connection == null) {
            System.out.println("Kunde inte ansluta till databasen");
            System.exit(-1);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        String input = "";

        do {
            // Meny för användaren baserat på om de är inloggade eller inte
            if (loggedInUserId == -1) {
                System.out.println("1. Skapa ny inloggning");
                System.out.println("2. Logga in");
            } else {
                System.out.println("3. Visa lista på böcker/Sök");
                System.out.println("4. Låna bok");
                System.out.println("5. Låna media");
                System.out.println("6. Lämna tillbaka");
                System.out.println("7. Lånestatus");
                System.out.println("8. Uppdatera din profil");
                System.out.println("9. Logga ut");
            }
            System.out.println("10. Avsluta");

            input = scanner.nextLine();

            // Switch-case för att hantera olika användaraktioner
            switch (input) {
                case "1":
                    System.out.println("Skapa ny inloggning");
                    registerUser(connection, scanner);
                    break;
                case "2":
                    System.out.println("Logga in");
                    logInUser(connection, scanner);
                    break;
                case "3":
                    System.out.println("Visa lista");
                    printBook(connection, scanner);
                    printMedia(connection, scanner);
                    break;
                case "4":
                    System.out.println("Låna bok");
                    borrowBook(connection, scanner);
                    break;
                case "5":
                    System.out.println("Låna media");
                    borrowMedia(connection, scanner);
                    break;
                case "6":
                    System.out.println("Lämna tillbaka");
                    returnBook(connection, scanner);
                    returnMedia(connection, scanner);
                    break;
                case "7":
                    System.out.println("Lånestatus");
                    viewBorrowedBooks(connection);
                    viewBorrowedMedia(connection);
                    break;
                case "8":
                    System.out.println("Uppdatera din profil");
                    updateProfile(connection, scanner);
                    break;
                case "9":
                    System.out.println("Logga ut");
                    logOut();
                    break;
            }
        } while (!input.equals("10"));
        System.out.println("Programmet avslutades");
    }

    // Metoden för att registrera en ny användare
    private static int registerUser(Connection connection, Scanner scanner) {
        // Användaren uppmanas att ange information för registrering
        System.out.println("Skapa ny inloggning:");
        System.out.print("Användarnamn: ");
        String username = scanner.nextLine();
        System.out.print("Lösenord: ");
        String password = scanner.nextLine();
        System.out.print("Namn: ");
        String name = scanner.nextLine();
        System.out.print("E-post: ");
        String email = scanner.nextLine();

        try {
            // Förbereder SQL-frågan för att lägga till en ny användare i databasen
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Users (username, password, name, email) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, email);

            // Utför SQL-frågan och få antalet påverkade rader
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                // Hämta genererade nycklar (i detta fall användarens ID) från den nya raden
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    // Registreringen lyckades, skriv ut användarens ID och returnera det
                    System.out.println("Registrering lyckades. Användare skapad med ID: " + generatedKeys.getInt(1));
                    return generatedKeys.getInt(1); // Returnera användarens id
                }
            }
        } catch (SQLException e) {
            // Vid SQL-fel, skriv ut felmeddelandet
            e.printStackTrace();
        }

        // Registrering misslyckades
        return -1;
    }

    // Metoden för att logga in användaren
    private static void logInUser(Connection connection, Scanner scanner) {
        // Användaren ska ange inloggningsinformation
        System.out.print("Användarnamn: ");
        String username = scanner.nextLine();
        System.out.print("Lösenord: ");
        String password = scanner.nextLine();

        try {
            // Förbereder SQL-frågan för att kontrollera användarens inloggningsuppgifter
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT user_id FROM Users WHERE username = ? AND password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Inloggning lyckades, sätter användarens ID i den globala variabeln
                loggedInUserId = resultSet.getInt("user_id");
                System.out.println("Inloggning lyckades. Användare inloggad med ID: " + loggedInUserId);
            } else {
                // Inloggning misslyckades, felaktigt användarnamn eller lösenord
                System.out.println("Inloggning misslyckades. Felaktigt användarnamn eller lösenord.");
            }
        } catch (SQLException e) {
            // Vid SQL-fel, skriv ut felmeddelandet
            e.printStackTrace();
        }
    }

    // Metoden för att skriva ut information om media
    private static void printMedia(Connection conn, Scanner scanner) {
        // SQL-fråga för att hämta all information från Media-tabellen
        String sql = "SELECT * FROM Media";
        try {
            // Skapar ett SQL-statement
            Statement statement= conn.createStatement();
            // Utför SQL-frågan och lagrar resultatet i en ResultSet
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                // Skriver ut information om varje rad i ResultSet
                System.out.println("media_id: " + rs.getString("media_id"));
                System.out.println("title: " + rs.getString("title"));
                System.out.println("available: " + rs.getString("available"));
                System.out.println("");
            }
        } catch (SQLException ex) {
            // Vid SQL-fel, skriv ut felmeddelandet
            Database.PrintSQLException(ex);
        }
    }
    // Metoden för att skriva ut information om böcker
    private static void printBook(Connection conn, Scanner scanner) {
        // SQL-fråga för att hämta all information från Books-tabellen
        String sql = "SELECT * FROM Books";

        try {
            // Skapar ett SQL-statement
            Statement statement = conn.createStatement();
            // Utför SQL-frågan och lagrar resultatet i en ResultSet
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()){
                // Skriver ut information om varje rad i ResultSet
                System.out.println("book_id: " + rs.getString("book_id"));
                System.out.println("title: " + rs.getString("title"));
                System.out.println("author: " + rs.getString("author"));
                System.out.println("available: " + rs.getString("available"));
                System.out.println("");
            }
        } catch(SQLException ex) {
            // Vid SQL-fel, skriv ut felmeddelandet
            Database.PrintSQLException(ex);
        }
    }
    // Metoden för att logga ut användaren
    private static void logOut() {
        // Återställer användar-ID för att indikera att ingen användare är inloggad
        loggedInUserId = -1;
        System.out.println("Utloggad");
    }
    // Metoden för att låna en bok
    private static void borrowBook(Connection connection, Scanner scanner) {
        // Användaren får ange book_id för boken de vill låna
        System.out.print("Ange book_id för boken du vill låna: ");
        int bookId = scanner.nextInt();
        scanner.nextLine();  // Konsumera radbrytningskaraktären

        try {
            // Kontrollera om boken är tillgänglig
            PreparedStatement checkAvailability = connection.prepareStatement("SELECT available FROM Books WHERE book_id = ?");
            checkAvailability.setInt(1, bookId);
            ResultSet availabilityResult = checkAvailability.executeQuery();

            if (availabilityResult.next()) {
                boolean isAvailable = availabilityResult.getBoolean("available");

                if (isAvailable) {
                    // Uppdatera boken som utlånad
                    PreparedStatement borrowBook = connection.prepareStatement("UPDATE Books SET available = false WHERE book_id = ?");
                    borrowBook.setInt(1, bookId);
                    int rowsAffected = borrowBook.executeUpdate();

                    if (rowsAffected > 0) {
                        // Beräkna förfallodatum (30 dagar från dagens datum)
                        LocalDate dueDate = LocalDate.now().plusDays(30);

                        // Registrera lånet i Loans-tabellen med förfallodatum
                        PreparedStatement recordLoan = connection.prepareStatement("INSERT INTO Loans (user_id, book_id, loan_date, due_date) VALUES (?, ?, NOW(), ?)");
                        recordLoan.setInt(1, loggedInUserId);
                        recordLoan.setInt(2, bookId);
                        recordLoan.setDate(3, Date.valueOf(dueDate));
                        recordLoan.executeUpdate();

                        System.out.println("Du har lånat boken. Glöm inte att lämna tillbaka i tid!");
                    } else {
                        System.out.println("Tyvärr, något gick fel vid lån av boken.");
                    }
                } else {
                    System.out.println("Tyvärr, boken är redan utlånad.");
                }
            } else {
                System.out.println("Boken med id " + bookId + " existerar inte.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Metoden för att lämna tillbaka en bok
    private static void returnBook(Connection connection, Scanner scanner) {
        // Användaren får ange book_id för boken de vill lämna tillbaka
        System.out.print("Ange book_id för boken du vill lämna tillbaka: ");
        int bookId = scanner.nextInt();
        scanner.nextLine();  // Konsumera radbrytningskaraktären

        try {
            // Kontrollera om boken är utlånad av den inloggade användaren
            PreparedStatement checkBorrowed = connection.prepareStatement("SELECT * FROM Loans WHERE user_id = ? AND book_id = ?");
            checkBorrowed.setInt(1, loggedInUserId);
            checkBorrowed.setInt(2, bookId);
            ResultSet borrowedResult = checkBorrowed.executeQuery();

            if (borrowedResult.next()) {
                // Uppdatera boken som tillgänglig
                PreparedStatement returnBook = connection.prepareStatement("UPDATE Books SET available = true WHERE book_id = ?");
                returnBook.setInt(1, bookId);
                int rowsAffected = returnBook.executeUpdate();

                if (rowsAffected > 0) {
                    // Registrera återlämningsdatum i Loans-tabellen
                    PreparedStatement recordReturn = connection.prepareStatement("UPDATE Loans SET return_date = NOW() WHERE user_id = ? AND book_id = ?");
                    recordReturn.setInt(1, loggedInUserId);
                    recordReturn.setInt(2, bookId);
                    recordReturn.executeUpdate();

                    System.out.println("Du har lämnat tillbaka boken. Tack!");
                } else {
                    System.out.println("Tyvärr, något gick fel vid återlämning av boken.");
                }
            } else {
                System.out.println("Du har inte lånat boken med id " + bookId + ".");
            }
            viewBorrowedBooks(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Metoden för att låna media
    private static void borrowMedia(Connection connection, Scanner scanner) {
        // Användaren får ange media_id för den media de vill låna
        System.out.print("Ange media_id för den media du vill låna: ");
        int mediaId = scanner.nextInt();
        scanner.nextLine();  // Konsumera radbrytningskaraktären

        try {
            // Kontrollera om media är tillgänglig
            PreparedStatement checkAvailability = connection.prepareStatement("SELECT available FROM Media WHERE media_id = ?");
            checkAvailability.setInt(1, mediaId);
            ResultSet availabilityResult = checkAvailability.executeQuery();

            if (availabilityResult.next()) {
                boolean isAvailable = availabilityResult.getBoolean("available");

                if (isAvailable) {
                    // Uppdatera media som utlånad
                    PreparedStatement borrowMedia = connection.prepareStatement("UPDATE Media SET available = false WHERE media_id = ?");
                    borrowMedia.setInt(1, mediaId);
                    int rowsAffected = borrowMedia.executeUpdate();

                    if (rowsAffected > 0) {
                        // Beräkna förfallodatum (10 dagar från dagens datum)
                        LocalDate dueDate = LocalDate.now().plusDays(10);

                        // Registrera lånet i LoansMedia-tabellen med förfallodatum
                        PreparedStatement recordLoan = connection.prepareStatement("INSERT INTO LoansMedia (user_id, media_id, loan_date, due_date) VALUES (?, ?, NOW(), ?)");
                        recordLoan.setInt(1, loggedInUserId);
                        recordLoan.setInt(2, mediaId);
                        recordLoan.setDate(3, Date.valueOf(dueDate));
                        recordLoan.executeUpdate();

                        System.out.println("Du har lånat media. Glöm inte att lämna tillbaka i tid!");
                    } else {
                        System.out.println("Tyvärr, något gick fel vid lån av media.");
                    }
                } else {
                    System.out.println("Tyvärr, media är redan utlånad.");
                }
            } else {
                System.out.println("Media med id " + mediaId + " existerar inte.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Metoden för att lämna tillbaka media
    private static void returnMedia(Connection connection, Scanner scanner) {
        // Användaren får ange media_id för den media de vill lämna tillbaka
        System.out.print("Ange media_id för den media du vill lämna tillbaka: ");
        int mediaId = scanner.nextInt();
        scanner.nextLine();  // Konsumera radbrytningskaraktären

        try {
            // Kontrollera om media är utlånad av inloggad användare
            PreparedStatement checkBorrowed = connection.prepareStatement("SELECT * FROM LoansMedia WHERE user_id = ? AND media_id = ?");
            checkBorrowed.setInt(1, loggedInUserId);
            checkBorrowed.setInt(2, mediaId);
            ResultSet borrowedResult = checkBorrowed.executeQuery();

            if (borrowedResult.next()) {
                // Uppdatera media som tillgänglig
                PreparedStatement returnMedia = connection.prepareStatement("UPDATE Media SET available = true WHERE media_id = ?");
                returnMedia.setInt(1, mediaId);
                int rowsAffected = returnMedia.executeUpdate();

                if (rowsAffected > 0) {
                    // Registrera återlämningsdatumet i LoansMedia-tabellen
                    PreparedStatement recordReturn = connection.prepareStatement("UPDATE LoansMedia SET return_date = NOW() WHERE user_id = ? AND media_id = ?");
                    recordReturn.setInt(1, loggedInUserId);
                    recordReturn.setInt(2, mediaId);
                    recordReturn.executeUpdate();

                    System.out.println("Du har lämnat tillbaka media. Tack!");
                } else {
                    System.out.println("Tyvärr, något gick fel vid återlämning av media.");
                }
            } else {
                System.out.println("Du har inte lånat media med id " + mediaId + ".");
            }
            viewBorrowedMedia(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Metoden för att uppdatera användarprofilen
    private static void updateProfile(Connection connection, Scanner scanner) {
        // Användaren väljer vad de vill uppdatera
        System.out.println("Välkommen till profilsidan! Vad vill du uppdatera?");
        System.out.println("1. Användarnamn");
        System.out.println("2. Lösenord");
        System.out.println("3. Namn");
        System.out.println("4. E-post");
        System.out.println("5. Avbryt");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                updateUsername(connection, scanner);
                break;
            case "2":
                updatePassword(connection, scanner);
                break;
            case "3":
                updateName(connection, scanner);
                break;
            case "4":
                updateEmail(connection, scanner);
                break;
            case "5":
                System.out.println("Avbryter uppdatering av profil.");
                break;
            default:
                System.out.println("Ogiltigt val.");
        }
    }
    // Metoden för att uppdatera användarnamnet
    private static void updateUsername(Connection connection, Scanner scanner) {
        System.out.print("Ange det nya användarnamnet: ");
        String newUsername = scanner.nextLine();

        try {
            // Förbered en SQL-uppdateringsfråga för användarnamnet
            PreparedStatement updateUsername = connection.prepareStatement("UPDATE Users SET username = ? WHERE user_id = ?");
            updateUsername.setString(1, newUsername);
            updateUsername.setInt(2, loggedInUserId);

            // Utför uppdateringen och hämta antalet påverkade rader
            int rowsAffected = updateUsername.executeUpdate();

            // Kontrollera om uppdateringen lyckades
            if (rowsAffected > 0) {
                System.out.println("Användarnamnet har uppdaterats.");
            } else {
                System.out.println("Tyvärr, något gick fel vid uppdatering av användarnamnet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Metoden för att uppdatera lösenordet
    private static void updatePassword(Connection connection, Scanner scanner) {
        System.out.print("Ange det nya lösenordet: ");
        String newPassword = scanner.nextLine();

        try {
            // Förbered en SQL-uppdateringsfråga för lösenordet
            PreparedStatement updatePassword = connection.prepareStatement("UPDATE Users SET password = ? WHERE user_id = ?");
            updatePassword.setString(1, newPassword);
            updatePassword.setInt(2, loggedInUserId);

            // Utför uppdateringen och hämta antalet påverkade rader
            int rowsAffected = updatePassword.executeUpdate();

            // Kontrollera om uppdateringen lyckades
            if (rowsAffected > 0) {
                System.out.println("Lösenordet har uppdaterats.");
            } else {
                System.out.println("Tyvärr, något gick fel vid uppdatering av lösenordet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Metoden för att uppdatera användarens namn
    private static void updateName(Connection connection, Scanner scanner) {
        System.out.print("Ange ditt nya namn: ");
        String newName = scanner.nextLine();

        try {
            // Förbered en SQL-uppdateringsfråga för namnet
            PreparedStatement updateName = connection.prepareStatement("UPDATE Users SET name = ? WHERE user_id = ?");
            updateName.setString(1, newName);
            updateName.setInt(2, loggedInUserId);

            // Utför uppdateringen och hämta antalet påverkade rader
            int rowsAffected = updateName.executeUpdate();

            // Kontrollera om uppdateringen lyckades
            if (rowsAffected > 0) {
                System.out.println("Namnet har uppdaterats.");
            } else {
                System.out.println("Tyvärr, något gick fel vid uppdatering av namnet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Metoden för att uppdatera användarens e-postadress
    private static void updateEmail(Connection connection, Scanner scanner) {
        System.out.print("Ange din nya e-post: ");
        String newEmail = scanner.nextLine();

        try {
            // Förbered en SQL-uppdateringsfråga för e-postadressen
            PreparedStatement updateEmail = connection.prepareStatement("UPDATE Users SET email = ? WHERE user_id = ?");
            updateEmail.setString(1, newEmail);
            updateEmail.setInt(2, loggedInUserId);

            // Utför uppdateringen och hämta antalet påverkade rader
            int rowsAffected = updateEmail.executeUpdate();

            // Kontrollera om uppdateringen lyckades
            if (rowsAffected > 0) {
                System.out.println("E-postadressen har uppdaterats.");
            } else {
                System.out.println("Tyvärr, något gick fel vid uppdatering av e-postadressen.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void viewBorrowedBooks(Connection connection) {
        try {
            // Prepare the SQL statement to get borrowed books for the logged-in user
            String sql = "SELECT Books.title AS item_title, Loans.loan_date, Loans.due_date " +
                    "FROM Books " +
                    "JOIN Loans ON Books.book_id = Loans.book_id " +
                    "WHERE Loans.user_id = ? AND Loans.return_date IS NULL";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, loggedInUserId);

            // Execute the query
            ResultSet resultSet = preparedStatement.executeQuery();

            // Display the borrowed books
            if (resultSet.next()) {
                System.out.println("Dina lånade böcker:");
                do {
                    String title = resultSet.getString("item_title");
                    String loanDate = resultSet.getString("loan_date");
                    String dueDate = resultSet.getString("due_date");

                    System.out.println("Titel: " + title);
                    System.out.println("Lånedatum: " + loanDate);
                    System.out.println("Förfallodatum: " + dueDate);
                    System.out.println();
                } while (resultSet.next());
            } else {
                System.out.println("Du har inga lånade böcker för närvarande.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewBorrowedMedia(Connection connection) {
        try {
            // Prepare the SQL statement to get borrowed media for the logged-in user
            String sql = "SELECT Media.title AS item_title, LoansMedia.loan_date, LoansMedia.due_date " +
                    "FROM Media " +
                    "JOIN LoansMedia ON Media.media_id = LoansMedia.media_id " +
                    "WHERE LoansMedia.user_id = ? AND LoansMedia.return_date IS NULL";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, loggedInUserId);

            // Execute the query
            ResultSet resultSet = preparedStatement.executeQuery();

            // Display the borrowed media
            if (resultSet.next()) {
                System.out.println("Dina lånade media:");
                do {
                    String title = resultSet.getString("item_title");
                    String loanDate = resultSet.getString("loan_date");
                    String dueDate = resultSet.getString("due_date");

                    System.out.println("Titel: " + title);
                    System.out.println("Lånedatum: " + loanDate);
                    System.out.println("Förfallodatum: " + dueDate);
                    System.out.println();
                } while (resultSet.next());
            } else {
                System.out.println("Du har inga lånade media för närvarande.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
