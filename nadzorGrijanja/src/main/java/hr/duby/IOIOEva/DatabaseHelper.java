package hr.duby.IOIOEva;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper{

	public static final String DATABASE_NAME = "employee_directory";
    
    public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
            /*
             * Create the employee table and populate it with sample data.
             * In step 6, we will move these hardcoded statements to an XML document.
             */
//            String sql = "CREATE TABLE IF NOT EXISTS employee (" +
//                                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                            "firstName TEXT, " +
//                                            "lastName TEXT, " +
//                                            "title TEXT, " +
//                                            "officePhone TEXT, " +
//                                            "cellPhone TEXT, " +
//                                            "email TEXT, " +
//                                            "managerId INTEGER)";
	//      values.put("firstName", "John");
	//      values.put("lastName", "Smith");
	//      values.put("title", "CEO");
	//      values.put("officePhone", "617-219-2001");
	//      values.put("cellPhone", "617-456-7890");
	//      values.put("email", "jsmith@email.com");
	//      db.insert("employee", "lastName", values);
    	
    	
//    	    String sql = "CREATE TABLE IF NOT EXISTS MyTable (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4))";
//            db.execSQL(sql);
//           
//            ContentValues values = new ContentValues();
//            
//            values.put("Datum", "17.10.2013 15:30:36");
//            values.put("Temp1S", "1200");
//            values.put("Temp2S", "1300");
//            values.put("Temp3S", "1400");
//            db.insert("MyTable", "Datum", values);




           
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS MyTable");
            onCreate(db);
    }


}
