package com.example.opencvtest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Contagem.db";
    public static final String TABLE_NAME = "contagem_bckp";
    public static final String ID = "ID";
    public static final String QUANTIDADE = "QUANTIDADE";
    public static final String NUMERO_FAIXA = "NUMERO_FAIXA";
    public static final String DATA_HORA = "DATA_HORA";
    public static final String ID_DISPOSITIVO = "ID_DISPOSITIVO";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME +
                        "(" +
                        "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "QUANTIDADE INTEGER," +
                        "NUMERO_FAIXA INTEGER," +
                        "DATA_HORA DATETIME," +
                        "ID_DISPOSITIVO VARCHAR(50)" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean inserirDados(String quantidade, String numero_faixa, String data_hora,String id_dispositivo){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put(QUANTIDADE, quantidade);
        content.put(NUMERO_FAIXA,numero_faixa);
        content.put(DATA_HORA,data_hora);
        content.put(ID_DISPOSITIVO,id_dispositivo);
        long result = db.insert(TABLE_NAME,null,content);

        if(result == -1){
            return false;
        }
        else {
            return  true;
        }
    }

    public Cursor buscarTodos(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor result = db.rawQuery("SELECT * FROM " + TABLE_NAME,null);
        return result;
    }

    public boolean atualizar(String id, String quantidade, String numero_faixa, String data_hora,String id_dispositivo){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put(ID, id);
        content.put(QUANTIDADE, quantidade);
        content.put(NUMERO_FAIXA,numero_faixa);
        content.put(DATA_HORA,data_hora);
        content.put(ID_DISPOSITIVO,id_dispositivo);
        db.update(TABLE_NAME,content,"ID = ?", new String[] {id});
        return true;
    }

    public Integer deletar(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        //Irá retornar a quantidade de linha afetadas pelo comando DELETE
        return db.delete(TABLE_NAME,"ID = ?", new String[] {id});
    }

}
