package com.example.opencvtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.text.DateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    Mat img1,
            img2,
            img3,
            result,
            element,
            element_dilate,
            shadow_image,
            flow;
    boolean cv_on = false,
            flag_imei=false,
            flag_dispositivo_realocado=false,
            flag_entrada_fluxo1 = false,
            flag_saida_fluxo1 = false,
            flag_habilita_contagem1=false,
            flag_entrada_fluxo2 = false,
            flag_saida_fluxo2 = false,
            flag_habilita_contagem2=false;
    List<Point> pontos_fluxo_faixa1 ;
    List<Point> pontos_fluxo_faixa2 ;
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    TelephonyManager tm;
    LocationManager locationManager;
    LocationListener locationListener;
    String IMEI,longitude_str,latitude_str;
    Double longitude,latitude;
    int pontos_superiores_fluxo[],pontos_inferiores_fluxo[],pontos_superiores_fluxo2[],pontos_inferiores_fluxo2[];
    Point ponto_ref1,ponto_ref2;
    // List<MatOfPoint> contours;
    //  List<MatOfPoint> goodContours;
     //BackgroundSubtractorMOG2 backgroundSubtractorMOG2;
    //  MatOfRect obj;
    // FeatureDetector blob ;
    // MatOfKeyPoint keypoints1;
    MatOfPoint2f prevFeatures, nextFeatures,prevFeatures2,nextFeatures2;
    MatOfPoint features,features2,teste;
    MatOfByte status;
    MatOfFloat err;
    Date tempoAntigo, tempoAtual, tempoAux;
    int cont = 0;
    int car_count_faixa1 = 0,car_count_faixa2 = 0,flag_envio=0;

    //Variaveis da conexao via socket
    Socket client,test_client;
    ObjectOutputStream ois,test_ois;
    String IP_digitado;

    //Coordenadas dos pontos das linhas do fluxo
    List<Point> coordLinhas = new ArrayList<Point>();
    Point finalLinha = new Point();
    boolean flag_ok_linhas = false;
    boolean flag_definindo_linhas = false;
    int qtdFaixas=0;
    int nRows1=0,nRows2=0,nRows=0;
    int rowStep = 30, colStep = 40,nCols =5,tamanhoLista=0;

    //Chamando o banco
    DatabaseHelper myDb;

    //API
    WebAPI webAPI;
    private TextView txt_view;



    /*Definindo o comportamento ao toque*/
    View.OnTouchListener handleTouch = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    finalLinha.x = x;
                    finalLinha.y = y - 180;

                    break;
                case MotionEvent.ACTION_UP:
                    if(!flag_ok_linhas) {
                        coordLinhas.add(new Point(x, y - 180)); /*Ajuste da coordeanda vertical, de forma a ficar na janela da imagem*/
                        myDb.inserirDadosCoordenadasPontos(Integer.toString(x),Integer.toString(y - 180));
                    }
                    break;
            }
            return true;
        }
    };
    /*Fim da definição do comportamento ao toque*/

    public void imprimirLinha(Point ponto_inicial,Point ponto_final, Mat imagem){
        Imgproc.line(imagem,ponto_inicial,ponto_final,new Scalar(255,255,0));
    }


    //Inicio funcoes dos botoes
    public void apply_cv(View view) {//Habilita o processamento das imagens

        cv_on = true;
        Date currentTime = Calendar.getInstance().getTime();
        CharSequence cs = Integer.toString(currentTime.getMinutes());
        Toast.makeText(getApplicationContext(),cs,Toast.LENGTH_SHORT).show();
    }
    public void remove_cv(View view){//Desabilita o processamento da imagem e reseta o contador dos carros
        if(cv_on){
            cv_on = false;
            Toast.makeText(getApplicationContext(),Integer.toString(car_count_faixa1), Toast.LENGTH_LONG).show();
            car_count_faixa1 = 0;
            car_count_faixa2 = 0;
        }
        else Toast.makeText(getApplicationContext(),"cv_on = false", Toast.LENGTH_LONG).show();

    }

    public void indica_realocacao(View view){
        flag_dispositivo_realocado = true;
        Toast.makeText(getApplicationContext(),"Dispositivo realocado = True",Toast.LENGTH_SHORT).show();
    }

    public void show_coordeninates(View view){
        if(latitude_str != null){
            Toast.makeText(getApplicationContext(),"lat,long: (" +latitude_str + "," + longitude_str + ")",Toast.LENGTH_SHORT ).show();
        }
    }
    public void show_imei(View view){
        if(IMEI != null){
            Toast.makeText(getApplicationContext(),"IMEI = " + IMEI,Toast.LENGTH_SHORT).show();
        }
    }
    /*Fim da criação das funções dos botões*/

    public void realizaConexao(String IMEI, String latitude, String longitude){
        try {
            client = new Socket("54.159.245.247", 65432);
            PrintWriter printWriter = new PrintWriter(client.getOutputStream());
            printWriter.write(IMEI + "," + latitude+ "," + longitude);
            printWriter.flush();
            printWriter.close();
            ois = new ObjectOutputStream(client.getOutputStream());
            //ois.writeChars("(1,teste)");
            ois.flush();
            ois.close();
            client.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void realizaConexao(String IMEI, String latitude, String longitude,char flagRealocacao){
        try {
            client = new Socket("54.159.245.247", 65432);
            PrintWriter printWriter = new PrintWriter(client.getOutputStream());
            printWriter.write(IMEI + "," + latitude+ "," + longitude+ "," + flagRealocacao);
            printWriter.flush();
            printWriter.close();
            ois = new ObjectOutputStream(client.getOutputStream());
            //ois.writeChars("(1,teste)");
            ois.flush();
            ois.close();
            client.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void apiGetDispositivos(){
        //Instanciando as funções da API
        //Chamando a API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://54.159.245.247:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webAPI = retrofit.create(WebAPI.class);

        Call<List<Dispositivo>> call = webAPI.getDispositivos();
        call.enqueue(new Callback<List<Dispositivo>>() {
            @Override
            public void onResponse(Call<List<Dispositivo>> call, Response<List<Dispositivo>> response) {
                if(!response.isSuccessful()){
                    txt_view.setText("Code:" + response.code());
                    return;
                }

                List<Dispositivo> dispositivos = response.body();

                for(Dispositivo dispositivo : dispositivos){
                    String content = "";
                    content += "id: " + dispositivo.getId() + "\n";
                    content += "marca_modelo " + dispositivo.getMarcaModelo() + "\n";
                    //content += "data_cadastro " + dispositivo.getDataCadasro()  + "\n";
                    content += "criado_por " + dispositivo.getCriadoPor()  + "\n";
                    content += "ativo " + dispositivo.getAtivo()  + "\n\n";

                    txt_view.append(content);
                }

            }

            @Override
            public void onFailure(Call<List<Dispositivo>> call, Throwable t) {
                txt_view.setText(t.getMessage());
            }
        });
    }

    public void apiGetDispositivos(String dispositivoId){
        //Instanciando as funções da API
        //Chamando a API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://54.159.245.247:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webAPI = retrofit.create(WebAPI.class);

        Call<List<Dispositivo>> call = webAPI.getDispositivos(dispositivoId);
        call.enqueue(new Callback<List<Dispositivo>>() {
            @Override
            public void onResponse(Call<List<Dispositivo>> call, Response<List<Dispositivo>> response) {
                if(!response.isSuccessful()){
                    txt_view.setText("Code:" + response.code());
                    return;
                }

                List<Dispositivo> dispositivos = response.body();

                for(Dispositivo dispositivo : dispositivos){
                    String content = "";
                    content += "id: " + dispositivo.getId() + "\n";
                    content += "marca_modelo " + dispositivo.getMarcaModelo() + "\n";
                    //content += "data_cadastro " + dispositivo.getDataCadasro()  + "\n";
                    content += "criado_por " + dispositivo.getCriadoPor()  + "\n";
                    content += "ativo " + dispositivo.getAtivo()  + "\n\n";

                    txt_view.append(content);
                }

            }

            @Override
            public void onFailure(Call<List<Dispositivo>> call, Throwable t) {
                txt_view.setText(t.getMessage());
            }
        });
    }

    public void apiGetLocalizacao(String dispositivoId){
        //Instanciando as funções da API
        //Chamando a API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://54.159.245.247:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webAPI = retrofit.create(WebAPI.class);

        Call<List<Localizacao>> call = webAPI.getLocalizacao(dispositivoId);
        call.enqueue(new Callback<List<Localizacao>>() {
            @Override
            public void onResponse(Call<List<Localizacao>> call, Response<List<Localizacao>> response) {
                if(!response.isSuccessful()){
                    txt_view.setText("Code:" + response.code());
                    return;
                }

                List<Localizacao> localizacoes = response.body();

                for(Localizacao localizacao : localizacoes){
                    String content = "";
                    content += "id_dispositivo: " + localizacao.getId_dispositivo() + "\n";
                    content += "latitude: " + localizacao.getLatitude() + "\n";
                    content += "longitude: " + localizacao.getLongitude() + "\n";
                    content += "num_faixas: " + localizacao.getQtdFaixas() + "\n\n";

                    txt_view.append(content);
                }

            }

            @Override
            public void onFailure(Call<List<Localizacao>> call, Throwable t) {
                txt_view.setText(t.getMessage());
            }
        });
    }

    public void apiPostTest(){
        //Instanciando as funções da API
        //Chamando a API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webAPI = retrofit.create(WebAPI.class);
        Post post = new Post(23,"Title","text");
        Call<Post> call = webAPI.createPost(post);
        call.enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {
                if(!response.isSuccessful()){
                    txt_view.setText("Code:" + response.code());
                    return;
                }
                Post postResponse = response.body();
                String content = "";
                content += "id: "+ postResponse.getId();
                content += "user_id: "+ postResponse.getUserId();
                content += "title: " + postResponse.getTitlte();
                content += "text: " + postResponse.getText();

                txt_view.append(content);
            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
                txt_view.setText(t.getMessage());
            }
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        /*Solicitando a permissao da camera*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
            }
        }

        //Instanciando o banco
        myDb = new DatabaseHelper(this);

        txt_view = findViewById(R.id.txt_view);

        //apiGetDspositivos("353114091675712");
        //apiGetLocalizacao("353114091675712");
        //apiPostTest();

        //Instanciando os botões da view
        final Button Bt_config = (Button) findViewById(R.id.Config);
        final Button bt_start = (Button) findViewById(R.id.Start);
        final Button bt_stop = (Button) findViewById(R.id.Stop);
        final Button bt_def_linhas = (Button) findViewById(R.id.def_linhas);
        final Button bt_confirmar_linhas = (Button) findViewById(R.id.confirmar_linhas);
        final Button bt_remover_linhas = (Button) findViewById(R.id.remover_linhas);


        bt_def_linhas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!flag_definindo_linhas) {
                    Bt_config.setVisibility(View.INVISIBLE);
                    bt_start.setVisibility(View.INVISIBLE);
                    bt_stop.setVisibility(View.INVISIBLE);
                    bt_confirmar_linhas.setVisibility(View.VISIBLE);
                    bt_remover_linhas.setVisibility(View.VISIBLE);
                    flag_definindo_linhas = true;
                    nRows1=0;
                    nRows2=0;
                    if(flag_ok_linhas){
                        flag_ok_linhas = false;
                    }
                    /*Habilitando a obseravação de toques na superficie da câmera*/
                    cameraBridgeViewBase.setOnTouchListener(handleTouch);
                }
                else{
                    Bt_config.setVisibility(View.VISIBLE);
                    bt_start.setVisibility(View.VISIBLE);
                    bt_stop.setVisibility(View.VISIBLE);
                    bt_confirmar_linhas.setVisibility(View.INVISIBLE);
                    bt_remover_linhas.setVisibility(View.INVISIBLE);
                    cameraBridgeViewBase.setOnTouchListener(null);
                    flag_definindo_linhas = false;
                }
            }
        });

        bt_confirmar_linhas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag_ok_linhas = true;
                qtdFaixas = coordLinhas.size()/2;
                Bt_config.setVisibility(View.VISIBLE);
                bt_start.setVisibility(View.VISIBLE);
                bt_stop.setVisibility(View.VISIBLE);
                bt_confirmar_linhas.setVisibility(View.INVISIBLE);
                bt_remover_linhas.setVisibility(View.INVISIBLE);
                cameraBridgeViewBase.setOnTouchListener(null);
                for(int k=0;k<coordLinhas.size();k=k+2){
                    if(k==0) {
                        nRows1 = ((int) coordLinhas.get(k + 1).y - (int) coordLinhas.get(k).y) / rowStep +1;
                    }
                    else if(k==2){
                        nRows2 = ((int) coordLinhas.get(k + 1).y - (int) coordLinhas.get(k).y) / rowStep +1;
                    }
                }
                if(nRows2!=0) {
                    Toast.makeText(getApplicationContext(), "Linhas1 = " + Integer.toString(nRows1) + "Linhas2 = " + Integer.toString(nRows2), Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Linhas1 = " + Integer.toString(nRows1), Toast.LENGTH_SHORT).show();
                }
            }
        });

        bt_remover_linhas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                coordLinhas.clear();
                pontos_fluxo_faixa1.clear();
                pontos_fluxo_faixa2.clear();
                features = new MatOfPoint();
                features2 = new MatOfPoint();
                int linhas_afetadas = myDb.deletar("coordenadas_pontos_bckp");
                if(linhas_afetadas>0){
                    Toast.makeText(getApplicationContext(),"Dados removidos com sucesso",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(),"Não há registros a serem removidos",Toast.LENGTH_SHORT).show();
                }

            }
        });


        Bt_config.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent inte = new Intent(MainActivity.this,Configuracoes.class);
                startActivity(inte);*/
                Cursor resultado_busca = myDb.buscarTodos("coordenadas_pontos_bckp");
                if(resultado_busca.getCount()>0){
                    while (resultado_busca.moveToNext()){
                        coordLinhas.add(new Point(resultado_busca.getInt(1),resultado_busca.getInt(2)));
                    }
                    Toast.makeText(getApplicationContext(),"Coordenadas recuperadas com sucesso", Toast.LENGTH_SHORT).show();
                    flag_ok_linhas = true;
                }
                else{
                    Toast.makeText(getApplicationContext(),"Não há dados registrados",Toast.LENGTH_SHORT).show();
                }
            }
        });


        /*Pegando o código do IMEI*/
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = tm.getDeviceId();

        /*obtendo as coordenadas do GPS*/
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                latitude_str = latitude.toString();
                longitude = location.getLongitude();
                longitude_str = longitude.toString();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        locationManager.requestLocationUpdates("gps",5000,0,locationListener);

        /*acessando data*/
        TextView textView=findViewById(R.id.data);
        DateFormat sdf = DateFormat.getDateTimeInstance();
        String currentDateandTime = sdf.format(new Date());
        textView.setText(currentDateandTime);

        //Coletando a data que o aplciativo começa a ser executado
        tempoAntigo = Calendar.getInstance().getTime();



        //Habilitando a camera
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);



        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"OpenCV foi carregado!!",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Falha ao abrir o OpenCV",Toast.LENGTH_SHORT).show();
        }

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;

                }


            }
        };
    }

    /*private void resetVars(){
        img2 = new Mat();
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
        if (!contours.isEmpty()) {
            contours.clear();
        }
        if(!backgroundSubtractorMOG2.empty()) {
            backgroundSubtractorMOG2.clear();
        }
    }*/
    //Função para comparar o tempo e realziar o envio para o servidor
    private Date compTime(Date dataAtual,Date dataAntiga){
        Date novaData;
        int difference =0;
        if(dataAntiga.getMinutes() == 50 && dataAtual.getMinutes() == 0){
            difference = 10;
        }
        else {
            difference = Math.abs(dataAntiga.getMinutes() - dataAtual.getMinutes());
        }

        if(difference == 10){//intervalo de tempo para envio dos dados
            novaData = dataAtual;
            cont++;
            Imgproc.putText(img1,"1 minuto",new Point(50,50),Core.FONT_ITALIC, 3.0,new Scalar(255));
            Imgproc.putText(img1,Integer.toString(cont),new Point(50,200),Core.FONT_ITALIC, 3.0,new Scalar(255));

            //socket connection
            //realizaConexao(IMEI,latitude_str,longitude_str);
        }
        else{
            novaData = dataAntiga;
        }
        return novaData;
    }

    //Função para realizar a ccomparação dos pontos do fluxo optico para contar os carros

    boolean comp_points(Point prevPoint, Point nextPoint, Point pontoSuperior, Point pontoInferior,int numFaixa){
       if(nextPoint.x < prevPoint.x){
           if(prevPoint.x == pontoSuperior.x ){
               if(Math.abs(nextPoint.x - prevPoint.x )>=3 && Math.abs(prevPoint.y - nextPoint.y) >=0 && Math.abs(prevPoint.y - nextPoint.y) <=2) {
                   if (numFaixa == 1) {
                       if(ponto_ref1 == null) {
                           flag_habilita_contagem1 = true;
                       }
                       flag_entrada_fluxo1 = true;
                       Imgproc.putText(img3, "entrou1", new Point(100, 100), Core.FONT_ITALIC, 2, new Scalar(255));
                   }
                   else {
                       if(ponto_ref2 == null) {
                           flag_habilita_contagem2 = true;
                       }
                       flag_entrada_fluxo2 = true;
                       Imgproc.putText(img3, "entrou2", new Point(100, 300), Core.FONT_ITALIC, 2, new Scalar(255));

                   }
               }
                else {
                    flag_entrada_fluxo1 = false;
                    flag_entrada_fluxo2 = false;
                }
           }

           //Se o objeto cruzar a linha inferior do fluxo
           else if(prevPoint.x == pontoInferior.x ){
               if (Math.abs(nextPoint.x - prevPoint.x )>=15 && Math.abs(prevPoint.y - nextPoint.y) >=0 && Math.abs(prevPoint.y - nextPoint.y) <=2) {
                   if (flag_habilita_contagem1 && !flag_saida_fluxo1 && numFaixa == 1) {
                       if(ponto_ref1 == null) {
                           ponto_ref1 = new Point(prevPoint.x,prevPoint.y);

                           flag_saida_fluxo1 = true;
                       }
                   }
                   else if (flag_habilita_contagem2 && !flag_saida_fluxo2 && numFaixa == 2) {
                       if(ponto_ref2 == null) {
                           ponto_ref2 = new Point(prevPoint.x,prevPoint.y);

                           flag_saida_fluxo2 = true;
                       }
                   }
               }
               //Se o obejto estiver saindo do fluxo e a distância entre os pontos for menor que o threshold
               else if(Math.abs(nextPoint.x - prevPoint.x )<2 && Math.abs(prevPoint.y - nextPoint.y) >=0 && Math.abs(prevPoint.y - nextPoint.y) <=2){
                   if(flag_saida_fluxo1  && numFaixa==1 && ponto_ref1.x == prevPoint.x && ponto_ref1.y == prevPoint.y ) {
                       flag_saida_fluxo1 = false;
                       flag_habilita_contagem1 = false;
                       ponto_ref1 = null;
                       return true;
                   }
                   else if(flag_saida_fluxo2 && flag_habilita_contagem2 && numFaixa==2 && ponto_ref2.x == prevPoint.x && ponto_ref2.y == prevPoint.y){
                       flag_saida_fluxo2 = false;
                       flag_habilita_contagem2 = false;
                       ponto_ref2 = null;
                       return true;
                   }
               }
           }

       }
       return false;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
       // resetVars();
        img1 = inputFrame.gray();
        int cx,cy;

        //Imgproc.GaussianBlur(img1,img1,new Size(5,5),5);

        if(cv_on){//Se o botão estiver pressionado
            tempoAtual = Calendar.getInstance().getTime();
            tempoAux =  compTime(tempoAtual,tempoAntigo);
            tempoAntigo = tempoAux;

            //Envio das coordenadas do dispositivo para preencher a localizacao
            /*if(flag_envio == 0) {
                if(flag_dispositivo_realocado) {
                    realizaConexao(IMEI,latitude_str,longitude_str,'1');
                }
                realizaConexao(IMEI,latitude_str,longitude_str,'0');

                flag_envio=1;
            }*/


            //backgroundSubtractorMOG2.apply(img1,img1);
            //Imgproc.Canny(img1,img1,70,210);

            Imgproc.GaussianBlur(img1,img1,new Size(3,3),2);
            Core.flip(img1.t(),img1,1);


            /*Teste optical flow2*/

            if(features.toArray().length==0) {
                //Usando unica linha
                /*int rowStep = 30;
                int nRows = 6;
                Point points[] = new Point[nRows];
                Point points2[] = new Point[nRows];

                for(int i=0; i<nRows; i++){
                    //Definindo o cojunto de pontos referentes aos limites superiores e inferirores da área de fluxo otico
                    points[i]=new Point(i +30, i*rowStep);
                    points2[i]=new Point(i +30, (i+10)*rowStep);
                }*/
                //Fim usando unica linha

                //int rowStep = 30, colStep = 40,nCols =5;
                if(qtdFaixas==2) {
                    if (nRows1 > nRows2) {
                        nRows = nRows1;
                    } else {
                        nRows = nRows2;
                    }
                }
                else nRows = nRows1;
                if(qtdFaixas==1){
                    //Cada vetor de pontos representa uma região para o cálculo do fluxo
                    Point points[] = new Point[nRows1*nCols]; //o 3 representa a quatidade de colunas desejadas para o fluxo
                    pontos_superiores_fluxo= new int[nRows1];
                    pontos_inferiores_fluxo = new int[nRows1];

                    for(int i=0; i<nRows; i++){
                        //Definindo o cojunto de pontos referentes aos limites superiores e inferirores da área de fluxo otico
                        pontos_superiores_fluxo[i] = (nCols - 1) + (i * nCols);
                        pontos_inferiores_fluxo[i] = i * nCols;
                        for(int j=0; j<nCols; j++){
                            points[i*nCols+j]=pontos_fluxo_faixa1.get(i*nCols+j);
                        }
                    }
                    features.fromArray(points);
                    prevFeatures.fromList(features.toList());
                }
                else if(qtdFaixas==2){
                    Point points[] = new Point[nRows1*nCols]; //o 3 representa a quatidade de colunas desejadas para o fluxo
                    pontos_superiores_fluxo= new int[nRows1];
                    pontos_inferiores_fluxo = new int[nRows1];
                    Point points2[] = new Point[nRows2 * nCols];
                    pontos_superiores_fluxo2= new int[nRows2];
                    pontos_inferiores_fluxo2 = new int[nRows2];


                    for(int i=0; i<nRows; i++){
                        //Definindo o cojunto de pontos referentes aos limites superiores e inferirores da área de fluxo otico
                        if(i<nRows1) {
                            pontos_superiores_fluxo[i] = (nCols - 1) + (i * nCols);
                            pontos_inferiores_fluxo[i] = i * nCols;
                        }
                        if(i<nRows2){
                            pontos_superiores_fluxo2[i] = (nCols - 1) + (i * nCols);
                            pontos_inferiores_fluxo2[i] = i * nCols;
                        }
                        for(int j=0; j<nCols; j++){
                            if(i<nRows1) points[i*nCols+j]=pontos_fluxo_faixa1.get(i*nCols+j);
                            if(i<nRows2) points2[i*nCols+j] = pontos_fluxo_faixa2.get(i*nCols+j); //definindo que a segunda região iá começar na linha 10 * colstep
                        }
                    }
                    features.fromArray(points);
                    features2.fromArray(points2);
                    prevFeatures.fromList(features.toList());
                    prevFeatures2.fromList(features2.toList());


                }

                img2 = img1.clone();

           }
            img3 = img1.clone();
            Scalar color = new Scalar(255);
            if(qtdFaixas==1) {
                nextFeatures.fromArray(prevFeatures.toArray());
                Video.calcOpticalFlowPyrLK(img2,img1, prevFeatures, nextFeatures, status, err);
                List<Point> prevList =features.toList(), nextList=nextFeatures.toList();
                tamanhoLista = prevList.size();

                for(int i = 0; i<tamanhoLista; i++){
                    Imgproc.line(img3,prevList.get(i), nextList.get(i), color);
                        if(i<pontos_superiores_fluxo.length){
                            if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[i]), prevList.get(pontos_inferiores_fluxo[i]), 1)) {
                                car_count_faixa1++;
                            }
                        }
                        else {
                            if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[pontos_superiores_fluxo.length - 1]), prevList.get(pontos_inferiores_fluxo[pontos_superiores_fluxo.length - 1]), 1)) {
                                car_count_faixa1++;
                            }
                        }
                }
            }
            else if(qtdFaixas==2) {
                nextFeatures.fromArray(prevFeatures.toArray());
                nextFeatures2.fromArray(prevFeatures2.toArray());

                //Processamento do fluxo optico
                Video.calcOpticalFlowPyrLK(img2, img1, prevFeatures, nextFeatures, status, err);
                Video.calcOpticalFlowPyrLK(img2, img1, prevFeatures2, nextFeatures2, status, err);

                List<Point> prevList = features.toList(), nextList = nextFeatures.toList();
                List<Point> prevList2 = features2.toList(), nextList2 = nextFeatures2.toList();

                if (prevList.size() > prevList2.size()) {
                    tamanhoLista = prevList.size();
                } else {
                    tamanhoLista = prevList2.size();
                }

                for(int i = 0; i<tamanhoLista; i++){
                    if(i<prevList.size()) Imgproc.line(img3,prevList.get(i), nextList.get(i), color);
                    if(i<prevList2.size()) Imgproc.line(img3,prevList2.get(i), nextList2.get(i), color);

                    if(i<prevList.size()){
                        if(i<pontos_superiores_fluxo.length){
                            if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[i]), prevList.get(pontos_inferiores_fluxo[i]), 1)) {
                                car_count_faixa1++;
                            }
                        }
                        else {
                            if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[pontos_superiores_fluxo.length - 1]), prevList.get(pontos_inferiores_fluxo[pontos_superiores_fluxo.length - 1]), 1)) {
                                car_count_faixa1++;
                            }
                        }
                    }

                    if(i<prevList2.size()){
                        if(i<pontos_superiores_fluxo2.length){
                            if (comp_points(prevList2.get(i), nextList2.get(i), prevList2.get(pontos_superiores_fluxo2[i]), prevList2.get(pontos_inferiores_fluxo2[i]), 2)) {
                                car_count_faixa2++;
                            }
                        }
                        else {
                            if (comp_points(prevList2.get(i), nextList2.get(i), prevList2.get(pontos_superiores_fluxo2[pontos_superiores_fluxo2.length - 1]), prevList2.get(pontos_inferiores_fluxo2[pontos_superiores_fluxo2.length - 1]), 2)) {
                                car_count_faixa2++;
                            }
                        }
                    }
                }


            }
            //Scalar color = new Scalar(255);
            //Gerar os pontos e linhas na tela
           /* for(int i = 0; i<tamanhoLista; i++){
                if(i<prevList.size()) Imgproc.line(img3,prevList.get(i), nextList.get(i), color);
                if(i<prevList2.size()) Imgproc.line(img3,prevList2.get(i), nextList2.get(i), color);

                if(i<prevList.size()){
                    if(i<pontos_superiores_fluxo.length){
                        if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[i]), prevList.get(pontos_inferiores_fluxo[i]), 1)) {
                            car_count_faixa1++;
                        }
                    }
                    else {
                        if (comp_points(prevList.get(i), nextList.get(i), prevList.get(pontos_superiores_fluxo[pontos_superiores_fluxo.length - 1]), prevList.get(pontos_inferiores_fluxo[pontos_superiores_fluxo.length - 1]), 1)) {
                            car_count_faixa1++;
                        }
                    }
                }

                if(i<prevList2.size()){
                    if(i<pontos_superiores_fluxo2.length){
                        if (comp_points(prevList2.get(i), nextList2.get(i), prevList2.get(pontos_superiores_fluxo2[i]), prevList2.get(pontos_inferiores_fluxo2[i]), 2)) {
                            car_count_faixa2++;
                        }
                    }
                    else {
                        if (comp_points(prevList2.get(i), nextList2.get(i), prevList2.get(pontos_superiores_fluxo2[pontos_superiores_fluxo2.length - 1]), prevList2.get(pontos_inferiores_fluxo2[pontos_superiores_fluxo2.length - 1]), 2)) {
                            car_count_faixa2++;
                        }
                    }
                }
            }*/

            img2 = img1.clone();
            Imgproc.putText(img3,"F1=" + Integer.toString(car_count_faixa1),new Point(200,200),Core.FONT_ITALIC ,1.0,new Scalar(255));
            Imgproc.putText(img3,"F2=" + Integer.toString(car_count_faixa2),new Point(200,300),Core.FONT_ITALIC ,1.0,new Scalar(255));
            /*fim teste optical flow2*/

            //Imgproc.threshold(img1,shadow_image,55,255,Imgproc.THRESH_BINARY_INV);

            //Imgproc.Canny(img1,result,70,210);
            //Imgproc.cvtColor(img1,img3,Imgproc.COLOR_GRAY2RGB);

           // Imgproc.Canny(result,result,70,210);
/*
            //Inicio CascadeDetector

            MatOfRect cars = new MatOfRect();

            if(mJavaDetector!=null){
                mJavaDetector.detectMultiScale(result,cars,1.1,2, Objdetect.CASCADE_SCALE_IMAGE,new Size(20,20));
            }
            else {
                Toast.makeText(getApplicationContext(),"Classificador não carregado", Toast.LENGTH_SHORT).show();
            }

            Rect[] carsArray = cars.toArray();

            for (int i = 0; i < carsArray.length; i++) {
                Imgproc.rectangle(result, carsArray[i].tl(), carsArray[i].br(), new Scalar(255, 0, 0), 3);
            }*/
            //FIm cascadeDetector

            //Imgproc.erode(result,result,element);
            //Core.absdiff(result,shadow_image,img2);


            //Imgproc.erode(result,result,element);
            //Imgproc.erode(result,result,element);
            //Imgproc.dilate(result,result,element_dilate);
            //Imgproc.dilate(result,result,element_dilate);
           // Imgproc.dilate(result,result,element_dilate);
            //Imgproc.dilate(result,result,element);
            //Imgproc.dilate(result,result,element);




/*
            Imgproc.findContours(result,contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

            for(int i=0;i<contours.size();i++){
                double contourArea = Imgproc.contourArea(contours.get(i));
                if(contourArea>2500) {
                    Rect rectangle = Imgproc.boundingRect(contours.get(i));
                    cx = (int)((rectangle.tl().x + rectangle.br().x)/2);
                    cy = (int)((rectangle.tl().y + rectangle.br().y)/2);

                   // Imgproc.circle(img1,new Point(cx/2,cy/2),3,new Scalar (120));
                    //Imgproc.putText(img1,Double.toString(contourArea),new Point(cx/2,cy/2),Core.FONT_HERSHEY_PLAIN,1.0,new Scalar(255));
                    Imgproc.rectangle(img1, rectangle.tl(), rectangle.br(), new Scalar(255, 0, 0), 1);
                    //Imgproc.drawContours(img1,contours,i,new Scalar(255,0,0));
                }

            }*/
            //Imgproc.line(img1,new Point(0,(int)img1.height()-10),new Point(img1.width(),img1.height()-10),new Scalar(255));

            //Imgproc.rectangle(result,new Point(0,result.height()/2),new Point(result.width()/2,result.height()),new Scalar(255,0,0));

           // backgroundSubtractorMOG2.getBackgroundImage(img2);

            //Core.absdiff(img1,img2,result);

            return img3;
        }
        else {
            Core.flip(img1.t(), img1, 1);
            //definindo as linhas linhas
            int nPontos;
            if(!coordLinhas.isEmpty() && flag_definindo_linhas) {
                if (!flag_ok_linhas) {
                    for (nPontos = 0; nPontos < coordLinhas.size(); nPontos++) {
                        Imgproc.circle(img1, coordLinhas.get(nPontos), 1, new Scalar(255, 152, 22));
                        /*para cada novo ponto, adiciona-lo ao pacote com a chave no formato: "PontoNx" e "PontoNy"*/
                    }
                    if (coordLinhas.size() % 2 == 0) {/*desenhar a linha na tela a cada par de pontos*/
                        imprimirLinha(coordLinhas.get(nPontos - 1), coordLinhas.get(nPontos - 2), img1);
                    } else {
                        imprimirLinha(coordLinhas.get(nPontos - 1), finalLinha, img1);
                    }
                }
                else{
                    for(int k=0;k<coordLinhas.size();k=k+2){
                        for(int i=(int)coordLinhas.get(k).y; i<(int)coordLinhas.get(k+1).y; i=i+rowStep){
                            for(int j=(int)coordLinhas.get(k).x; j<(int)coordLinhas.get(k).x+(nCols*colStep); j=j+colStep){
                                Imgproc.circle(img1,new Point(j,i),1,new Scalar(255,255,255));
                                if(k==0) {
                                    pontos_fluxo_faixa1.add(new Point(j, i));
                                }
                                else if(k==2){
                                    pontos_fluxo_faixa2.add(new Point(j, i));
                                }
                            }
                        }
                    }
                }
            }

            //Definindo as linhas
            return img1;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        img1 = new Mat();
        img2 = new Mat();
        img3 = new Mat();
        flow = new Mat();
        shadow_image = new Mat();
        element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2,2),new Point(0,0));
        element_dilate = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3),new Point(0,0));
        result = new Mat();
       // pontos = new ArrayList<Point>();
       // obj=new MatOfRect();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        prevFeatures2 = new MatOfPoint2f();
        nextFeatures2 = new MatOfPoint2f();
        pontos_fluxo_faixa1 = new ArrayList<>();
        pontos_fluxo_faixa2 = new ArrayList<>();
        status = new MatOfByte();
        //backgroundSubtractorMOG2 = Video.createBackgroundSubtractorMOG2();
       // contours = new ArrayList<MatOfPoint>();
        features = new MatOfPoint();
        features2 = new MatOfPoint();
        teste = new MatOfPoint();
        err = new MatOfFloat();
        //backgroundSubtractorMOG2.setHistory(100);
       // backgroundSubtractorMOG2.setDetectShadows(true);
       //backgroundSubtractorMOG2.setShadowThreshold(0.4);
        //backgroundSubtractorMOG2.setVarThresholdGen(15);
        //backgroundSubtractorMOG2.setComplexityReductionThreshold(0.2);
       //backgroundSubtractorMOG2.setVarThreshold(10);
       //backgroundSubtractorMOG2.setBackgroundRatio(0.7);
        //backgroundSubtractorMOG2.setShadowValue(255);



        //backgroundSubtractorMOG2.setVarInit(5);
        /*Toast.makeText(getApplicationContext(),"Ate aqui roda",Toast.LENGTH_LONG).show();*/
        //blob = FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);
        /*Toast.makeText(getApplicationContext(),"Ate aqui roda2",Toast.LENGTH_LONG).show();*/
        //keypoints1 = new MatOfKeyPoint();
       //inicio cascade
/*
        try {
            InputStream is = getResources().openRawResource(R.raw.cars);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "new_cars.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            cascadeDir.delete();
        }
        catch (IOException e) {
            e.printStackTrace();
        }*/
        //Fim cascade

    }


    @Override
    public void onCameraViewStopped() {

        img1.release();
        img2.release();
        img3.release();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"Erro ao abrir o OpenCV", Toast.LENGTH_SHORT).show();
        }
        else{
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(cameraBridgeViewBase!=null) {
            cameraBridgeViewBase.disableView();
            img1.release();
            img2.release();
            img3.release();
        }
    }


}
