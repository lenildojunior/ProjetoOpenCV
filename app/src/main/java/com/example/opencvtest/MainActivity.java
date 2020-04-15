package com.example.opencvtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import java.net.Socket;
import java.text.DateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    Mat img1, img2, img3, result, element, element_dilate, shadow_image, flow;
    boolean cv_on = false,flag_imei=false,flag_dispositivo_realocado=false;
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    TelephonyManager tm;
    LocationManager locationManager;
    LocationListener locationListener;
    String IMEI,longitude_str,latitude_str;
    Double longitude,latitude;
    List<Point> pontos;
    // List<MatOfPoint> contours;
    //  List<MatOfPoint> goodContours;
    // BackgroundSubtractorMOG2 backgroundSubtractorMOG2;
    //  MatOfRect obj;
    // FeatureDetector blob ;
    // MatOfKeyPoint keypoints1;
    MatOfPoint2f prevFeatures, nextFeatures;
    MatOfPoint features;
    MatOfByte status;
    MatOfFloat err;
    Date tempoAntigo, tempoAtual, tempoAux;
    int cont = 0;
    int car_count = 0,flag_envio=0;

    Socket client,test_client;
    ObjectOutputStream ois,test_ois;
    String IP_digitado;


    /*Teste cascade calssifier*/
    //File mCascadeFile;
    //CascadeClassifier mJavaDetector;
    //float mRelativeFaceSize   = 0.2f;
    // int mAbsoluteFaceSize   = 0;


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
            Toast.makeText(getApplicationContext(),Integer.toString(car_count), Toast.LENGTH_LONG).show();
            car_count = 0;
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
        }




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

        //Habilitando o campo para receber o ip do servidor
        final EditText ET = (EditText) findViewById(R.id.Enter_text);
        final Button Bt_enter = (Button) findViewById(R.id.Enter_button);

        Bt_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ET.setVisibility(View.INVISIBLE);
                Bt_enter.setVisibility(View.INVISIBLE);
                if (!ET.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(),"Texto não nulo", Toast.LENGTH_SHORT).show();
                    IP_digitado = ET.getText().toString();
                }
            }
        });



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
        if(dataAntiga.getMinutes() == 58 && dataAtual.getMinutes() == 0){
            difference = 2;
        }
        else {
            difference = Math.abs(dataAntiga.getMinutes() - dataAtual.getMinutes());
        }

        if(difference == 1){//intervalo de tempo para envio dos dados
            novaData = dataAtual;
            cont++;
            Imgproc.putText(img1,"1 minuto",new Point(50,50),Core.FONT_ITALIC, 3.0,new Scalar(255));
            Imgproc.putText(img1,Integer.toString(cont),new Point(50,200),Core.FONT_ITALIC, 3.0,new Scalar(255));

            //socket connection
            try {
                client = new Socket(IP_digitado, 65432);
                PrintWriter printWriter = new PrintWriter(client.getOutputStream());
                printWriter.write(IMEI + "," + latitude_str + "," + longitude_str);
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

            //Toast.makeText(getApplicationContext(),"1 minuto",Toast.LENGTH_SHORT).show();
        }
        else{
            novaData = dataAntiga;
        }
        return novaData;
    }

    //Função para realizar a ccomparação dos pontos do fluxo optico para contar os carros
    boolean comp_points(Point prevPoint, Point nextPoint){
       if(nextPoint.x < prevPoint.x && Math.abs(nextPoint.x - prevPoint.x )>10){
           return true;
       }
       else return false;
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
            if(flag_envio == 0) {
                try {
                    test_client = new Socket(IP_digitado, 65432);
                    PrintWriter printWriter2 = new PrintWriter(test_client.getOutputStream());
                    if(flag_dispositivo_realocado) {
                        printWriter2.write(IMEI + "," + latitude_str + "," + longitude_str + "," + '1');
                    }
                    else printWriter2.write(IMEI + "," + latitude_str + "," + longitude_str + "," + '0');
                    printWriter2.flush();
                    printWriter2.close();
                    test_ois = new ObjectOutputStream(test_client.getOutputStream());
                    //ois.writeChars("(1,teste)");
                    test_ois.flush();
                    test_ois.close();
                    test_client.close();


                } catch (IOException e) {
                    e.printStackTrace();
                }
                flag_envio=1;
            }


            //backgroundSubtractorMOG2.apply(img1,img3);

        /*Teste optical flow*/
            /*if(features.toArray().length==0) {
                Imgproc.goodFeaturesToTrack(result, features, 10, 0.01, 10);
                prevFeatures.fromList(features.toList());
                img2 = result.clone();
            }
            Video.calcOpticalFlowPyrLK(result,img2,prevFeatures,nextFeatures,status,err);
            List<Point> drawFeature = nextFeatures.toList();
            for(int i = 0; i<drawFeature.size(); i++){
                Point p = drawFeature.get(i);
                Imgproc.circle(result, p, 5, new Scalar(255));
            }
            img2 = result.clone();
            prevFeatures.fromList(nextFeatures.toList());*/

        /*fim teste optical flow*/
            Imgproc.GaussianBlur(img1,img1,new Size(5,5),5);
            Core.flip(img1.t(),img1,1);

            /*Teste optical flow2*/

            if(features.toArray().length==0) {
                int rowStep = 40, colStep = 40;
                int nRows = img1.rows()/rowStep, nCols = (img1.cols()/2)/colStep;
                //Point points[] = new Point[nRows*nCols];
                Point points[] = new Point[nRows*3];
                for(int i=0; i<nRows; i++){
                    //for(int j=0; j<nCols; j++){
                    for(int j=0; j<3; j++){
                        points[i*3+j]=new Point(j*colStep, i*rowStep);
//                            Log.d(TAG, "\nRow: "+i*rowStep+"\nCol: "+j*colStep+"\n: ");
                    }
                }
                features.fromArray(points);


                prevFeatures.fromList(features.toList());
                img2 = img1.clone();
           }
            nextFeatures.fromArray(prevFeatures.toArray());
            Video.calcOpticalFlowPyrLK(img2,img1, prevFeatures, nextFeatures, status, err);
            //Video.calcOpticalFlowFarneback(img2,img1,prevFeatures,0.5,3,15,3,5,1.2,0);
            List<Point> prevList=features.toList(), nextList=nextFeatures.toList();
            Scalar color = new Scalar(255);

            for(int i = 0; i<prevList.size(); i++){
                    //Imgproc.circle(img1, prevList.get(i), 5, color);
                Imgproc.line(img1,prevList.get(i), nextList.get(i), color);
                if(comp_points(prevList.get(i),nextList.get(i))){
                    if(i<prevList.size()-3){//Verifica se não está na última linha
                        if(comp_points(prevList.get(i+3),nextList.get(i+3))){
                            car_count++;
                        }
                    }
                    else{
                        if(comp_points(prevList.get(i-3),nextList.get(i-3))){
                            car_count++;
                        }
                    }
                }
            }

            img2 = img1.clone();

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




            return img1;
        }
        /*Imgproc.Canny(img1,img1,70,210);*/
        /*blob.detect(img1,keypoints1);
        Features2d.drawKeypoints(img1,keypoints1,result,new Scalar(240,120,10));*/
        else {
            Core.flip(img1.t(), img1, 1);
            //Imgproc.rectangle(img1,new Point(0,img1.height()/2),new Point(img1.width()/2,img1.height()),new Scalar(255,0,0));

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
        status = new MatOfByte();
        //backgroundSubtractorMOG2 = Video.createBackgroundSubtractorMOG2();
       // contours = new ArrayList<MatOfPoint>();
        features = new MatOfPoint();
        err = new MatOfFloat();
       // backgroundSubtractorMOG2.setHistory(100);
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
