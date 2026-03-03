# Java-Desk_Booking-Tool

I developed this application to address a common workplace challenge: managing shared office spaces efficiently. As a Master’s student in Big Data at the West University of Timișoara, I wanted to go beyond basic coding and build something that follows professional security and data management standards.

Why I built this?
The goal was to create a production-ready desktop tool. Instead of just focusing on the UI, I spent a lot of time on the backend logic and how the app handles sensitive data.

Google Authentication: I implemented GoogleAuthService.java because nobody wants to manage yet another set of credentials. Integrating the OAuth 2.0 flow was a great way to learn how modern apps handle user identity.

Database Management with MongoDB: I chose MongoDB because its flexibility fits perfectly with the Big Data concepts I am currently studying.

Automated Cleanup: To keep the system running smoothly, I added a cleanOldBookings function that automatically removes reservations older than 30 days.

Security and Privacy: I used Dotenv to handle all API keys and database strings. This ensures that sensitive information stays in a local .env file and is never exposed in the public code.

The Technical Side:

Language: Java 17.

Database: MongoDB Atlas (NoSQL).

Dependencies: Managed through Maven.

Logging: I included a server logging system (server_logs) to track activity and make troubleshooting much easier.
