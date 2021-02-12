package com.currency.currency_converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import static com.currency.currency_converter.JDBC.*;

@SpringBootApplication
public class CurrencyConverterApplication implements CommandLineRunner {

	// Function to print text on the terminal
	private static Logger LOG = LoggerFactory.getLogger(CurrencyConverterApplication.class);

	public static void main(String[] args) {
		LOG.info("STARTING THE APPLICATION");
		SpringApplication.run(CurrencyConverterApplication.class, args);
		LOG.info("APPLICATION FINISHED");
	}

	// Initialization of the database in memory mode and return it
	public Connection initDatabase() throws Exception {
		Connection connection = null;
		try {
			connection = getConnectionFromDataSource();
			//Initialization of all database's tables
			initTable(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	// Insert all information of the API into the database with the given database connection
	public void insertAPIResponse(Connection connection) throws Exception {
		// Initialization of the API
		WebClient api = WebClient.create("https://api.exchangeratesapi.io/latest");

		// Recover all information and put them in CurrencyReponse class.
		CurrencyResponse currency = api.get()
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(CurrencyResponse.class)
				.block();

		// Insert CurrencyReponse's base into the databse (Base Table)
		insertBase(currency.getBase(), currency.getDate(), connection);

		// Insert all CurrencyReponse's rates into the databse (Rate Table)
		currency.getRates().forEach((name, value) -> {
			try {
				insertRate(name, value, currency.getBase(), connection);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// Display all information of the database on the terminal with the given database connection
	public void displayDatabase(Connection connection) throws Exception {
		// Get all bases of base table
		String requestSelectBase = "SELECT * FROM base;";
		ResultSet resultat = executerRequete(connection, requestSelectBase);

		// Display a table on the terminal with all information
		System.out.println("=".repeat( 32 ) + "Bases table" + "=".repeat( 32 ) + "▇");
		System.out.printf("%-25s%-25s%-25s|\n","idBase","name","date");

		while (resultat.next())
		{
			String idBase = resultat.getString("idBase");
			String name = resultat.getString("name");
			String date = resultat.getString("date");
			System.out.printf("%-25s%-25s%-25s|\n", idBase, name, date);
		}

		System.out.println("=".repeat( 75 ) + "▇\n");

		// Get all rates of rate table in relation to the base
		String requestSelectRate = "SELECT * FROM rate;";
		resultat = executerRequete(connection, requestSelectRate);

		// Display a table on the terminal with all information
		System.out.println("=".repeat( 44 ) + "Rates table" + "=".repeat( 45 ) + "▇");
		System.out.printf("%-25s%-25s%-25s%-25s|\n","idRate","name","value", "idBase");

		while (resultat.next()) {
			String idRate = resultat.getString("idRate");
			String name = resultat.getString("name");
			String value = resultat.getString("value");
			String idBase = resultat.getString("idBase");
			System.out.printf("%-25s%-25s%-25s%-25s|\n", idRate, name, value, idBase);
		}
		System.out.println("=".repeat( 100 ) + "▇\n");
	}

	// Converter of currency
	// float number --> the value of money to convert
	// String currency_from --> the starting currency
	// String currency_to --> the currency to reach
	// String base --> the base of currency rates
	// Connection connection --> the connection of the database
	public void currencyConverter(float number, String currency_from, String currency_to, String base, Connection connection) throws Exception {
		float ret = 0;
		// If the starting currency is the base, we just have to multiply by the rate of the currency to reach
		if (currency_from == "EUR") {
			ret = number * getRateByNameAndBase(currency_to, base, connection);
		// Else we have to divide by the rate of the starting currency to get back to the base, then multiply by the rate of the currency to reach
		}else {
			ret = (number / getRateByNameAndBase(currency_from, base, connection)) * getRateByNameAndBase(currency_to, base, connection);
		}
		System.out.println(number + " " + currency_from + " to " + currency_to + " is equal to " + ret);
	}

	// Some examples of currency conversion
	public void someConversionExamples(String base, Connection connexion) throws Exception {
		currencyConverter(42, "EUR", "USD", base, connexion);
		currencyConverter(71, "EUR", "GBP", base, connexion);
		currencyConverter(80, "NZD", "RON", base, connexion);
		currencyConverter(37, "DKK", "EUR", base, connexion);
	}

	// Function to list all rates in relation to the base and user can select one
	public int currencyChoice(String base, Connection connection) throws Exception {
		// Get all rate in relation to the currency base
		String requestSelectRate = "SELECT * FROM rate WHERE idBase = '" + getIdBaseByName(base, connection) + "';";
		ResultSet resultat = executerRequete(connection, requestSelectRate);
		int cpt = 0;
		ArrayList<Integer> idsRate = new ArrayList<>();

		// Show each rate and put their id in the idsRate array
		while (resultat.next()) {
			cpt++;
			String name = resultat.getString("name");
			String value = resultat.getString("value");
			idsRate.add(resultat.getInt("idRate"));
			System.out.printf("%-25s", cpt + ": " + name + " (" + value + ")");

			// Every 5 rates puts a break line
			if (cpt % 5 == 0) {
				System.out.println();
			}
		}
		// Show and put the base (the base has -2 for id)
		cpt++;
		idsRate.add(-2);
		System.out.printf("%-25s", cpt + ": " + base + " (1.0)");
		System.out.println();

		// Ask user to choose a currency
		Scanner scanner = new Scanner(System.in);
		System.out.println("Which currency do you want to use ? (If you want to quit type -1)");

		int response = -1;

		// Check if user response is an integer and an existing currency
		while (scanner.hasNext()) {
			if (scanner.hasNextInt()) {
				response = scanner.nextInt();
				if ((response >= 1 && response <= idsRate.size()) || response == -1) {
					break;
				}
			}else scanner.next();
			System.out.println("The response must be an integer between 1 and " + idsRate.size() + " or -1 to quit.");
		}

		// -1 to quit else it's a rate id
		return response == -1 ? -1 : idsRate.get(response - 1);
	}

	@Override
	public void run(String... args) throws Exception {
		// Set the base to EUR
		String base = "EUR";

		Connection connection = initDatabase();
		insertAPIResponse(connection);
		displayDatabase(connection);
		someConversionExamples(base, connection);

		// Ask user to convert currency
		while (true) {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Enter a value for your currency that you want to convert : (If you want to quit type -1)");

			// Check if the user's response is a float
			while (!scanner.hasNextFloat()) {
				scanner.next();
				System.out.println("The response must be a float.");
			}

			// Get the user's response
			float currency_value = scanner.nextFloat();
			// If it's -1 quit the app
			if (currency_value == -1) {
				break;
			}

			System.out.println("Currency at start :");
			// Ask user to choose a starting currency
			int currency_from = currencyChoice(base, connection);
			// If it's -1 quit the app
			if (currency_from == -1) {
				break;
			}

			System.out.println("Currency at the end :");
			// Ask user to choose a currency to reach
			int currency_to = currencyChoice(base, connection);
			// If it's -1 quit the app
			if (currency_to == -1) {
				break;
			}

			// Convert and display the conversion
			// If currency_from or currency_to equals -2 then this is the base
			currencyConverter(
					currency_value,
					currency_from == -2 ? base : getRateNameById(currency_from, connection),
					currency_to == -2 ? base : getRateNameById(currency_to, connection),
					base,
					connection
			);
		}
	}
}
