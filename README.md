# Java Desk Booking Tool

I developed this application to address a common workplace challenge: managing shared office spaces efficiently. Coming from an economics background and currently pursuing a Master's in Big Data, my goal was to go beyond basic coding and build a functional tool that respects professional security and data management standards.

Instead of just focusing on the user interface, I spent a significant amount of time on the backend logic, data persistence, and how the app handles sensitive information.

## Key Features & Architecture

* **Google Authentication:** I implemented Google OAuth 2.0 (`GoogleAuthService`) because users prefer not to manage yet another set of passwords. It was also a great hands-on way to learn how modern applications handle identity verification.
* **Database Management:** I chose MongoDB Atlas for this project. Its NoSQL flexibility fits perfectly with the data concepts I am currently studying.
* **Automated Cleanup:** To keep the database optimized, I wrote a background function that automatically purges reservations older than 30 days.
* **Security First:** I used Dotenv to manage all API keys and database connection strings. This ensures that sensitive information stays in a local `.env` file and is never pushed to the public repository.
* **Activity Logging:** I included a custom server logging system (`server_logs`) to track backend activity and make troubleshooting easier.

## Technical Stack

* **Language:** Java 25
* **Framework:** JavaFX
* **Database:** MongoDB Atlas
* **Dependency Management:** Maven

## How to run it locally

If you want to test this project on your machine, you will need to set up your own MongoDB cluster and generate Google Cloud API credentials.

1. Clone this repository.
2. Create a `.env` file in the root directory and add your variables:
   ```env
   MONGO_URI=your_mongodb_connection_string
   GOOGLE_CLIENT_ID=your_google_api_client_id
3. Build the project using Maven: mvn clean install
4. Run the application via AppLauncher.java.
