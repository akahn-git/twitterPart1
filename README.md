# Project 2 - SimpleTwitter

SimpleTwitter is an android app that allows a user to view his Twitter timeline. The app utilizes [Twitter REST API](https://dev.twitter.com/rest/public).

Time spent: 12 hours spent in total

## User Stories

The following **required** functionality is completed:

- [.] User can **sign in to Twitter** using OAuth login
- [.]	User can **view tweets from their home timeline**
- [.] User is displayed the username, name, and body for each tweet
- [.] User is displayed the [relative timestamp](https://gist.github.com/nesquena/f786232f5ef72f6e10a7) for each tweet "8m", "7h"
- [.] User can refresh tweets timeline by pulling down to refresh

The following **optional** features are implemented:

- [.] User can view more tweets as they scroll with infinite pagination
- [.] Improve the user interface and theme the app to feel "twitter branded"
- [.] Links in tweets are clickable and will launch the web browser
- [ ] User can tap a tweet to display a "detailed" view of that tweet
- [ ] User can see embedded image media within the tweet detail view
- [ ] User can watch embedded video within the tweet
- [ ] User can open the twitter app offline and see last loaded tweets
- [ ] On the Twitter timeline, leverage the CoordinatorLayout to apply scrolling behavior that hides / shows the toolbar.

The following **additional** features are implemented:

- [ ] List anything else that you can get done to improve the app functionality!

## Video Walkthrough

Here's a walkthrough of implemented user stories:

![twitter](https://user-images.githubusercontent.com/98711133/195465925-9779f1c7-8414-450e-b84e-bb018c29553c.gif)

GIF created with [LiceCap](http://www.cockos.com/licecap/).

## Notes

Describe any challenges encountered while building the app.

## Open-source libraries used

- [Android Async HTTP](https://github.com/codepath/CPAsyncHttpClient) - Simple asynchronous HTTP requests with JSON parsing
- [Glide](https://github.com/bumptech/glide) - Image loading and caching library for Android

## License

    Copyright [2022] [Alex-Kahn]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
```

Note we are using `getApiUrl` to get the full URL from the relative fragment and `RequestParams` to control the request parameters.
You can easily send post requests (or put or delete) using a similar approach:

```kotlin
// RestClient.kt
fun postTweet(body: String, handler: JsonHttpResponseHandler) {
    val apiUrl = getApiUrl("statuses/update.json")
    val params = RequestParams()
    params.put("status", body)
    client.post(apiUrl, params, handler)
}
```

These endpoint methods will automatically execute asynchronous requests signed with the authenticated access token. To use JSON endpoints, simply invoke the method
with a `JsonHttpResponseHandler` handler:

```kotlin
// SomeActivity.kt
val client = RestApplication.getRestClient()
client.getHomeTimeline(object : JsonHttpResponseHandler() {
    override fun onSuccess(statusCode: Int, headers: okhttp3.Headers, json: JSON) {
        Log.i(TAG, "onSuccess!")
        // parse json response here
    }

    override fun onFailure(
        statusCode: Int,
        headers: okhttp3.Headers,
        response: String,
        throwable: Throwable
    ) {
        Log.i(TAG, "onFailure!", throwable)
    }

})
```

Based on the JSON response (array or object), you need to declare the expected type inside the OnSuccess signature i.e
`public void onSuccess(JSONObject json)`. If the endpoint does not return JSON, then you can use the `AsyncHttpResponseHandler`:

```kotlin
val client = RestApplication.getRestClient()
client.getSomething(object : JsonHttpResponseHandler() {
    override fun onSuccess (statusCode: Int, headers: Headers, json: JSON) {
        System.out.println(json)
    }
});
```
Check out [Android Async HTTP Docs](https://github.com/codepath/AsyncHttpClient) for more request creation details.

### 2. Define the Models

In the `src/com.codepath.apps.restclienttemplate.models`, create the models that represent the key data to be parsed and persisted within your application.

For example, if you were connecting to Twitter, you would want a Tweet model as follows:

```kotlin
// models/Tweet.kt
package com.codepath.apps.restclienttemplate.models

import androidx.room.ColumnInfo
import androidx.room.Embedded

import androidx.room.Entity
import androidx.room.PrimaryKey

import org.json.JSONException
import org.json.JSONObject

@Entity
class Tweet {
  // Define database columns and associated fields
  @PrimaryKey
  @ColumnInfo
  var id: Long = 0L

  @ColumnInfo
  var userHandle: String = ""

  @ColumnInfo
  var timestamp: String = ""

  @ColumnInfo
  var body: String = ""

  // Use @Embedded to keep the column entries as part of the same table while still
  // keeping the logical separation between the two objects.
  @Embedded
  @Nullable
  var user: User = null
}
```

Note there is a separate `User` object but it will not actually be declared as a separate table.  By using the `@Embedded` annotation, the fields in this class will be stored as part of the Tweet table.  Room specifically does not load references between two different entities for performance reasons (see https://developer.android.com/training/data-storage/room/referencing-data), so declaring it this way causes the data to be denormalized as one table.

```kotlin
// models/User.java

class User {

    @ColumnInfo
    var name: String + ""

    // normally this field would be annotated @PrimaryKey because this is an embedded object
    // it is not needed
    @ColumnInfo
    var twitter_id: Long = 0L
}
```
Notice here we specify the SQLite table for a resource, the columns for that table, and a constructor for turning the JSON object fetched from the API into this object. For more information on creating a model, check out the [Room guide](https://developer.android.com/training/data-storage/room/).

In addition, we also add functions into the model to support parsing JSON attributes in order to instantiate the model based on API data.  For the User object, the parsing logic would be:

```kotlin
// Parse model from JSON
companion object {
    fun parseJSON(tweetJson: JSONObject) : User {
        val user = User()
        this.twitter_id = tweetJson.getLong("id")
        this.name = tweetJson.getString("name")
        return user
    }
}
```

For the Tweet object, the logic would would be:

```kotlin
// models/Tweet.kt
@Entity
companion object {
    fun fromJson(jsonObject: JSONObject): Tweet {
        val tweet = Tweet()
        tweet.body = jsonObject.getString("text")
        tweet.createdAt = jsonObject.getString("created_at")
        tweet.user = User.fromJson(jsonObject.getJSONObject("user"))
        return tweet
    }

    fun fromJsonArray(jsonArray: JSONArray): List<Tweet> {
        val tweets = ArrayList<Tweet>()
        for (i in 0 until jsonArray.length()) {
            tweets.add(fromJson(jsonArray.getJSONObject(i)))
        }
        return tweets
    }
}
```


Now you have a model that supports proper creation based on JSON. Create models for all the resources necessary for your mobile client.

### 4. Define your queries

Next, you will need to define the queries by creating a Data Access Object (DAO) class.   Here is an example of declaring queries to return a Tweet by the post ID, retrieve the most recent tweets, and insert tweets.

```kotlin

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import java.util.List

@Dao
interface TwitterDao {
    // @Query annotation requires knowing SQL syntax
    // See http://www.sqltutorial.org/
    @Query("SELECT * FROM SampleModel WHERE id = :id")
    fun byTweetId(tweetId: Long): Tweet?

    @Query("SELECT * FROM Tweet ORDER BY created_at")
    fun getRecentTweets(): List<Tweet>

    // Replace strategy is needed to ensure an update on the table row.  Otherwise the insertion will
    // fail.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTweet(tweets: List<Tweet>);
}
```

The examples here show how to perform basic queries on the Tweet table.  If you need to declare one-to-many or many-to-many relations, see the guides on using the [@Relation](https://developer.android.com/reference/android/arch/persistence/room/Relation) and [@ForeignKey](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey) annotations.

### 5. Create database

We need to define a database that extends `RoomDatabase` and describe which entities as part of this database. We also need to include what data access objects are to be included.  If the entities are modified or additional ones are included, the version number will need to be changed.  Note that only the `Tweet` class is declared:

```kotlin
// bump version number if your schema changes
@Database(entities={Tweet.class}, version=1)
abstract class MyDatabase : RoomDatabase() {
    abstract fun sampleModelDao(): SampleModelDao?

    companion object {
        // Database name to be used
        const val NAME = "MyDataBase"
    }
}
```

When compiling the code, the schemas will be stored in a `schemas/` directory assuming this statement
has been included your `app/build.gradle` file.  These schemas should be checked into your code based.

```gradle
android {
    defaultConfig {

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
            }
        }
    }

}
```

### 6. Initialize database

Inside your application class, you will need to initialize the database and specify a name for it.

```kotlin
class RestClientApp : Application {

  var myDatabase: MyDatabase = null

  override fun onCreate() {
    // when upgrading versions, kill the original tables by using fallbackToDestructiveMigration()
    myDatabase = Room.databaseBuilder(this, MyDatabase.class, MyDatabase.NAME).fallbackToDestructiveMigration().build()
  }

  fun getMyDatabase(): MyDatabase {
    return myDatabase
  }

}
```

### 7. Setup Your Authenticated Activities

Open `src/com.codepath.apps.restclienttemplate/LoginActivity.kt` and configure the `onLoginSuccess` method
which fires once your app has access to the authenticated API. Launch an activity and begin using your REST client:

```kotlin
// LoginActivity.kt
override fun onLoginSuccess() {
  val intent = Intent(this, TimelineActivity:class.java)
  startActivity(i);
}
```

In your new authenticated activity, you can access your client anywhere with:

```kotlin
val client = RestApplication.getRestClient()
client.getHomeTimeline(object : JsonHttpResponseHandler() {
  fun onSuccess(statusCode: Int, headers: Headers , json: JSON) {
    Log.d("DEBUG", "timeline: " + json.jsonArray.toString())
    // Load json array into model classes
  }
});
```

You can then load the data into your models from a `JSONArray` using:

```kotlin
val tweets = Tweet.fromJSON(jsonArray)
```

or load the data from a single `JSONObject` with:

```kotlin
val tweet = Tweet(json)
// t.body = "foo"
```

To save, you will need to perform the database operation on a separate thread by creating an `AsyncTask` and adding the item:

```kotlin
val task = object: AsyncTask<Tweet, Void, Void>() {
    override fun doInBackground(List<Tweet> tweets) {
      val twitterDao = (getApplicationContext() as RestApplication).getMyDatabase().twitterDao()
      twitterDao.insertModel(tweets)
      return null;
    };
  };
  task.execute(tweets);
```

That's all you need to get started. From here, hook up your activities and their behavior, adjust your models and add more REST endpoints.

### Extras

#### Loading Images with Glide

If you want to load a remote image url into a particular ImageView, you can use Glide to do that with:

```kotlin
Glide.with(this).load(imageUrl)
     .into(imageView);
```

This will load an image into the specified ImageView and resize the image to fit.

#### Logging Out

You can log out by clearing the access token at any time through the client object:

```kotlin
val client = RestApplication.getRestClient()
client.clearAccessToken()
```

### Viewing SQL table

You can use `chrome://inspect` to view the SQL tables once the app is running on your emulator.  See [this guide](https://guides.codepath.com/android/Debugging-with-Stetho) for more details.

### Adding OAuth2 support

Google uses OAuth2 APIs so make sure to use the `GoogleApi20` instance:

```kotlin
companion object {
  const val REST_API_INSTANCE = GoogleApi20.instance()
}
```

Change `REST_URL` to use the Google API:

```java
public static final String REST_URL = "https://www.googleapis.com/calendar/v3"; // Change this, base API URL
```

The consumer and secret keys should be retrieved via [the credentials section](https://console.developers.google.com/apis/credentials) in the Google developer console  You will need to create an OAuth2 client ID and client secret.

Create a file called `apikey.properties`:

```kotlin
REST_CONSUMER_KEY="XXX-XXX.apps.googleusercontent.com"
REST_CONSUMER_SECRET="XX-XXXXXXX"
```

The OAuth2 scopes should be used according to the ones defined in [the OAuth2 scopes](https://developers.google.com/identity/protocols/googlescopes):

```kotlin
companion object {
  const val OAUTH2_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
}
```

Make sure to pass this value into the scope parameter:

```kotlin
constructor RestClient(context: Context) {
		this(context, REST_API_INSTANCE,
				REST_URL,
				REST_CONSUMER_KEY,
				REST_CONSUMER_SECRET,
				OAUTH2_SCOPE,  // OAuth2 scope, null for OAuth1
				String.format(REST_CALLBACK_URL_TEMPLATE, context.getString(R.string.intent_host),
						context.getString(R.string.intent_scheme), context.getPackageName(), FALLBACK_URL));
	}
```
Google only accepts `http://` or `https://` domains, so your `REST_CALLBACK_URL_TEMPLATE` will need to be adjusted:

```kotlin
companion object {
  const val REST_CALLBACK_URL_TEMPLATE = "https://localhost"
}
```

Make sure to update the `cprest` and `intent_host` to match this callback URL .

### Troubleshooting

* If you receive the following error `org.scribe.exceptions.OAuthException: Cannot send unauthenticated requests for TwitterApi client. Please attach an access token!` then check the following:
 * Is your intent-filter with `<data>` attached to the `LoginActivity`? If not, make sure that the `LoginActivity` receives the request after OAuth authorization.
 * Is the `onLoginSuccess` method being executed in the `LoginActivity`. On launch of your app, be sure to start the app on the LoginActivity so authentication routines execute on launch and take you to the authenticated activity.
 * If you are plan to test with Android API 24 or above, you will need to use Chrome to launch the OAuth flow.
 * Note that the emulators (both the Google-provided x86 and Genymotion versions) for API 24+ versions can introduce intermittent issues when initiating the OAuth flow for the first time.  For best results, use an device for this project.
