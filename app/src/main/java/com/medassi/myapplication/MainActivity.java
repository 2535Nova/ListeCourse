package com.medassi.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;

public class MainActivity extends AppCompatActivity {
    public static RequestQueue queue ;
    public static String ragondin ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = Volley.newRequestQueue(this);

        Button b = findViewById(R.id.button);
        b.setOnClickListener(view -> clickButtonConnexion());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void clickButtonConnexion() {
        String pseudo = ((EditText) findViewById(R.id.editTextText)).getText().toString();
        String password = ((EditText) findViewById(R.id.editTextTextPassword)).getText().toString();

        // Construire l'URL avec les paramètres de requête
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=connexion_utilisateur&pseudo=" + pseudo + "&password=" + password;

        JsonObjectRequest reqHttp = new JsonObjectRequest(Request.Method.GET,
                url,
                null,
                jsonObject -> traiterRetourConnexion(jsonObject),
                volleyError -> traiterErreur(volleyError)
        );
        queue.add(reqHttp);
    }


    private void traiterErreur(VolleyError volleyError) {
        Toast.makeText(this, "Erreur HTTP :"+volleyError.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void traiterRetourConnexion(JSONObject jsonObject) {
        try {
            String request = jsonObject.getString("request") ;
            Boolean result = jsonObject.getBoolean("result") ;
            if( request.equals("connexion")){
                if( result ){
                    Toast.makeText(this, "Succés de la connexion", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity2.class) ;
                    ragondin = ((EditText) findViewById(R.id.editTextText)).getText().toString() ;
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "Connexion échouée", Toast.LENGTH_SHORT).show();
                    Log.wtf("CONNEXION WS" , "Oh ben merde") ;
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }
}