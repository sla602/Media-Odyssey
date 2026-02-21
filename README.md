# Media Odyssey

Link to GitHub: https://github.com/sla602/Media-Odyssey  
Link to Render: https://media-odyssey.onrender.com

## Project abstract

Media Odyssey (MO) is a browser-based web application designed to help users discover movies, songs, and games without needing to search across multiple social media platforms. Instead of passively scrolling through unrelated content, users can actively explore curated recommendation boards centred around specific interests such as moods, hobbies, genres, or personality traits. By combining personalised recommendation logic, community interaction, and external media APIs, Media Odyssey aims to create a focused, social, and customizable media discovery experience. The application is intended for general audiences and emphasises both entertainment and meaningful discovery.

## The Problem

Users want to discover movies, music, and games specific to their taste and interests, but media discovery is currently fragmented across multiple platforms. This makes it difficult for users to explore content that is personalised and curated for them without jumping from website to website.

Currently, users rely on individual media discovery platforms to discover new content, each limited to a single media type. Recommendation systems are engagement-driven and lack user personalisation. As a result, users must manually navigate between platforms and curate recommendations themselves.

## How will this project make life better? Is it educational or just for entertainment?

Media Odyssey is a website that allows users to create personalised theme boards that combine movies, songs, and games in one place. The project helps users organise their interests, discover new media, and understand their own preferences across multiple entertainment categories rather than just one. By bringing different media types together, users can explore connections between their tastes and find recommendations through shared boards and community interaction.

The platform is primarily designed for entertainment and discovery, but it also has educational value. Users can learn about new genres, cultures, and creative works through curated themes and shared collections. The social and exploratory features also encourage users to reflect on their preferences and broaden their media exposure, making the experience both enjoyable and informative.

## Target Audience

The target audience is general users who enjoy digital media such as movies, music, and games. Media Odyssey is especially helpful for people who want an easier and more organised way to discover content based on their interests. It is particularly suitable for students and young adults who have unique tastes, often check trending media, and like discussing their favourite content with others who share similar tastes online.

## Does this project have many individual features or one main feature?

This project consists of multiple major features rather than a single main feature, with each feature representing a distinct epic. These epics include user authentication and roles, media boards and layout, recommendation logic, social and community interaction, and community trends and discovery.

### User Accounts

User accounts are the foundation of Media Odyssey. By remembering users’ favourites and preferences, we can tailor the experience to meet the user’s needs, while also providing sufficient options to allow user discretion. It is important that new users are provided an excellent experience while creating an account, onboarding to the platform, and have a starting place to begin. First impressions matter, and Media Odyssey aims to be the #1 platform. Likewise, it is also important to facilitate a smooth experience with our existing users, where removing as much friction with single sign-on with common third-party accounts (ex. Google) and allowing users to “Remember me” in the same browser.

When users register/sign in using a third-party account (ex. Google), the API of that service will be used (ex. Google API). Where possible, email addresses can be verified by sending an email using one of the many SMTP relay services (ex. Resend). However, there can be challenges in obtaining a sending email address.

### Recommendation System

Media Odyssey will have a recommendation engine that utilises user data collected from queries, user content preferences, and engagement history. The engine will deliver dynamic recommendations that evolve with the user’s taste in media. Creating an environment for discovering new media, connecting users with art. The engine will be used throughout the different pages of the website to deliver media-specific recommendations on the different pages (Movies, Music and Games). AI-assisted recommendations will be explored using external services in tandem with APIs that fetch information about the media.

### Social Features

The user demands recommendations for content based on their personality to explore in-depth with other individuals in a community. Users have more autonomy to create a community for their own entertainment interests. The user can create tags for the visibility of their community in the search bar. A user will immediately get potential friend recommendations when joining other dashboards, based on prior mutual communities. When the user interacts with a community board, there will be user-defined emojis available to react to other users. User profiles will have personalised profile pictures and a brief list of top communities the user frequents. The user’s most recent posts will be displayed as the main part of the user profile.

### Community Favourites Discovery Page

The Community Favourites Discovery Page displays the Top 10 games, movies, and songs using the same ranking method for all media types. Items are sorted based on a combination of likes and views, and each entry shows a thumbnail, title, and engagement summary such as “1.2K likes” and “5K views,” along with an overall rating out of 5. When a logged-in user submits a rating, it is added to the total score in real time. Users can switch between Games, Movies, and Songs through JavaScript-powered tabs without reloading the page, and a real-time search bar filters the Top 10 lists as the user types. The page also includes a Trending section that highlights the Top 5 fastest-rising items using recent growth compared to last week’s data, with badges like “Up 20% in 24h” to help users quickly spot what is gaining popularity.

Media information for games, movies, and songs will be fetched using the same API methods as in the recommendation system.

### Board layouts

Media Odyssey aims to be a place for users to categorise their interests or discover songs, games, or movies that relate to their preferences. To provide a better experience, whenever users find interest in a song, game, or movie, descriptions of what they click on will be displayed.

For movies, a description of the plot, genre, directors, and actors will be shown, as well as the platforms where the movie is available for streaming. In the case of a song, the song’s lyrics and artist will be displayed. If a game is selected, it will only show basic information such as the genre, age restriction, and where to download the game. We will also suggest related content based on what the user clicked on, allowing users more space to explore.

To respect user preferences, Media Odyssey allows users to choose their colour theme, such as light, dark, or midnight, for the webpage. It will also have a news inbox where users can receive update announcements from Media Odyssey and the boards they follow. For this feature, APIs for fetching movie, song, and game descriptions or links will be used to ensure newly released movies, songs, and games are kept up to date.

## Sufficient for a five-member team?

We believe that the amount of work is sufficient for five group members. Although the complete feature list appears ambitious, it is intended to be completed over multiple iterations. There are multiple major features, suitable for a team of five. The project is also resizeable, where there are optional minor features that can be added or removed, as required. For example, sending messages between users can enhance the experience by allowing easier communication, but it is not a required feature because Media Odyssey’s focus is not a messaging app. It also provides an opportunity to understand which features work and which do not.