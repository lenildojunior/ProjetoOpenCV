package com.example.opencvtest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Contagem.db";

    //Dados da tabela para armazenar a contagem
    public static final String ID = "ID";
    public static final String QUANTIDADE = "QUANTIDADE";
    public static final String NUMERO_FAIXA = "NUMERO_FAIXA";
    public static final String DATA_HORA = "DATA_HORA";

    //Tabela para armazenar os pontos da faixa
    public static final String POS_X= "POS_X";
    public static final String POS_Y = "POS_Y";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE contagem_bckp" +
                        "(" +
                        "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "QUANTIDADE INTEGER," +
                        "NUMERO_FAIXA INTEGER," +
                        "DATA_HORA DATETIME" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE coordenadas_pontos_bckp" +
                        "(" +
                        "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "POS_X INTEGER," +
                        "POS_Y INTEGER" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS contagem_bckp");
        db.execSQL("DROP TABLE IF EXISTS coordenadas_pontos_bckp");
        onCreate(db);
    }

    public boolean inserirDadosContagem(String quantidade, String numero_faixa, String data_hora){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put(QUANTIDADE, quantidade);
        content.put(NUMERO_FAIXA,numero_faixa);
        content.put(DATA_HORA,data_hora);
        long result = db.insert("contagem_bckp",null,content);

        if(result == -1){
            return false;
        }
        else {
            return  true;
        }
    }

    public boolean inserirDadosCoordenadasPontos(String posX, String posY){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put(POS_X, posX);
        content.put(POS_Y, posY);
        long result = db.insert("coordenadas_pontos_bckp",null,content);

        if(result == -1){
            return false;
        }
        else {
            return  true;
        }
    }

    public Cursor buscarTodos(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor result = db.rawQuery("SELECT * FROM " + TABLE_NAME,null);
        return result;
    }

    public boolean atualizar(String id, String quantidade, String numero_faixa, String data_hora, String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put(ID, id);
        content.put(QUANTIDADE, quantidade);
        content.put(NUMERO_FAIXA,numero_faixa);
        content.put(DATA_HORA,data_hora);
        db.update(TABLE_NAME,content,"ID = ?", new String[] {id});
        return true;
    }

    public Integer deletar(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        //Irá retornar a quantidade de linha afetadas pelo comando DELETE
        return db.delete(TABLE_NAME,"ID > 0",null);
    }

}
