package com.sanlin.bay2keyboard;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_WORDS = "words";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_WORD = "word";
	public static final String COLUMN_FREQ = "freq";

	private static final String DATABASE_NAME = "words.db";
	private static final int DATABASE_VERSION = 1;

	private Context mContext;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table " + TABLE_WORDS
			+ "(" + COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_WORD + " text not null, " + COLUMN_FREQ
			+ " integer not null);";

	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		initializeDB(database);
	}

	private void initializeDB(SQLiteDatabase database) {
		
		XmlResourceParser parser = mContext.getResources().getXml(R.xml.wordslist);
				try {
					while (parser.next() != XmlPullParser.END_TAG) {
					    if (parser.getEventType() != XmlPullParser.START_TAG) {
					        continue;
					    }
					    String name = parser.getName();
					    String word;
					    int freq;
					    if (name.equals("word")) {
					    	String freqS = parser.getAttributeValue(null, "freq");
					    	word=readText(parser);
					        
					        if(!freqS.equals("1")){
					        	freqS="2";
					        }
					        ContentValues values = new ContentValues();
					        values.put(MySQLiteHelper.COLUMN_WORD, word);
							values.put(MySQLiteHelper.COLUMN_FREQ, freqS);
							long insertId = database.insert(MySQLiteHelper.TABLE_WORDS, null,
									values);
							
					    }
					}
				} catch (XmlPullParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
	}

	private String readText(XmlPullParser parser) throws IOException,
			XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORDS);
		onCreate(db);
	}

}