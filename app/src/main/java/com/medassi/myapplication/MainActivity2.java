package com.medassi.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private Spinner rayonsSpinner;
    private ListView listeProduitsListView;
    private ListView listeCadieListView;
    private RequestQueue requestQueue;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Affiche le nom d'utilisateur
        ((TextView)findViewById(R.id.textView)).setText(MainActivity.ragondin);

        // Initialiser la file de requêtes Volley
        requestQueue = Volley.newRequestQueue(this);

        // Ajoute un écouteur de clic au bouton de déconnexion
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            // Ferme cette activité et retourne à l'activité précédente (MainActivity)
            finish();
        });
        // Ajoute un écouteur de clic au bouton de reset
        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(view -> {
            // Appelle la fonction resetterProduits() lorsque le bouton de reset est cliqué
            resetterProduits();
        });

        // Ajuste le padding pour tenir compte de la barre de système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialiser le Spinner des rayons
        rayonsSpinner = findViewById(R.id.rayonsSpinner);
        chargerRayons();

        // Initialiser la ListView des produits
        listeProduitsListView = findViewById(R.id.listeProduitsListView);
        chargerProduits();
        // Initialize the ListView for the cart
        listeCadieListView = findViewById(R.id.listeCartListView);
        chargerCaddie();

        // Ajoute un écouteur de clic au bouton d'ajout de rayon
        Button ajouterRayonButton = findViewById(R.id.ajouterRayonButton);
        ajouterRayonButton.setOnClickListener(view -> {
            // Récupérer les valeurs des champs
            String nomProduit = ((EditText)findViewById(R.id.nomProduitEditText)).getText().toString();
            String commentaire = ((EditText)findViewById(R.id.commentaireEditText)).getText().toString();
            String selectedRayon = rayonsSpinner.getSelectedItem().toString();

            // Trouver l'ID du rayon sélectionné
            int rayonId = trouverIdRayon(selectedRayon);

            // Ajouter le produit
            ajouterProduit(nomProduit, commentaire, rayonId);
        });
        listeCadieListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Récupérez l'ID du produit à supprimer
                String selectedProduct = (String) parent.getItemAtPosition(position);
                String[] words = selectedProduct.split(" ");
                String firstWord = words[0];
                trouverIdProduitCaddie(firstWord, position);
                return true;
            }
        });

        // Ajoute un écouteur de long clic à la ListView des produits
        listeProduitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = (String) parent.getItemAtPosition(position);

                // Récupérer uniquement le premier mot du nom du produit
                String[] words = selectedProduct.split(" ");
                String firstWord = words[0];


                // Créer un AlertDialog pour demander à l'utilisateur s'il souhaite transférer ou supprimer le produit
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
                builder.setTitle("Options du produit");
                builder.setMessage("Que voulez-vous faire avec le produit sélectionné ?");

                builder.setPositiveButton("Transféré", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Récupérer le nom du produit à partir du premier mot
                        trouverIdProduit(firstWord);
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("Supprimer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Récupérer le nom du produit à partir du premier mot
                        trouverIdProduit(firstWord);
                        dialog.dismiss();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return true; // Retourne true pour indiquer que l'événement a été consommé
            }
        });


        // Initialize the handler
        handler = new Handler(Looper.getMainLooper());

        // Start the repeated task
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Refresh the list of products
                chargerProduits();

                // Schedule the next refresh in 2 minutes
                handler.postDelayed(this, 2 * 60 * 1000); // 2 minutes in milliseconds
            }
        }, 2 * 60 * 1000); // Start the first refresh after 2 minutes
    }

    private void chargerRayons() {
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=rayons";

        // Faire la demande HTTP pour obtenir la liste des rayons
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("rayons response", response.toString());
                        List<String> rayonsList = new ArrayList<>();
                        try {
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject rayonObject = jsonArray.getJSONObject(i);
                                String nomRayon = rayonObject.getString("nom_rayon");
                                rayonsList.add(nomRayon);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Remplir le Spinner avec la liste des rayons
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity2.this, android.R.layout.simple_spinner_item, rayonsList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        rayonsSpinner.setAdapter(adapter);
                    }}, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Gérer les erreurs de chargement des rayons
                error.printStackTrace();
            }
        });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(request);
    }

    private void chargerProduits() {
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=courses";

        // Faire la demande HTTP pour obtenir la liste des produits
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("produits response", response.toString());
                        List<String> produitsList = new ArrayList<>();
                        try {
                            JSONArray resultArray = response.getJSONArray("result");
                            for (int i = 0; i < resultArray.length(); i++) {
                                JSONObject produitObject = resultArray.getJSONObject(i);
                                String nomProduit = produitObject.getString("nom");
                                String commentaire = produitObject.getString("commentaire");
                                int rayonId = produitObject.getInt("rayon_id");
                                String nomRayon = getRayonName(rayonId);
                                if (!commentaire.isEmpty()) {
                                    nomProduit += " (" + commentaire + ")";
                                }
                                produitsList.add(nomProduit + " - " + nomRayon); // Ajouter le nom du produit et le rayon à la liste
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Remplir le ListView avec la liste des produits
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity2.this, android.R.layout.simple_list_item_1, produitsList);
                        listeProduitsListView.setAdapter(adapter);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Gérer les erreurs de chargement des produits
                error.printStackTrace();
            }
        });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(request);
    }

    // Add the chargerCaddie() method here
    private void chargerCaddie() {
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=caddie";

        // Faire la demande HTTP pour obtenir la liste des produits dans le caddie
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("caddie response", response.toString());
                        List<String> caddieList = new ArrayList<>();
                        try {
                            JSONArray resultArray = response.getJSONArray("result");
                            for (int i = 0; i < resultArray.length(); i++) {
                                JSONObject caddieObject = resultArray.getJSONObject(i);
                                String nomProduit = caddieObject.getString("nom");
                                String commentaire = caddieObject.getString("commentaire");
                                int rayonId = caddieObject.getInt("rayon_id");
                                String nomRayon = getRayonName(rayonId);
                                if (!commentaire.isEmpty()) {
                                    nomProduit += " (" + commentaire + ")";
                                }
                                caddieList.add(nomProduit + " - " + nomRayon); // Ajouter le nom du produit et le rayon à la liste
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Remplir le ListView avec la liste des produits dans le caddie
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity2.this, android.R.layout.simple_list_item_1, caddieList);
                        ListView caddieListView = findViewById(R.id.listeCartListView);
                        caddieListView.setAdapter(adapter);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Gérer les erreurs de chargement des produits dans le caddie
                error.printStackTrace();
            }
        });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(request);
    }

    private int trouverIdRayon(String nomRayon) {
        // Récupérer la liste des rayons du Spinner
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) rayonsSpinner.getAdapter();
        // Parcourir la liste des rayons pour trouver celui qui correspond au nom donné
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(nomRayon)) {
                // Récupérer l'ID du rayon correspondant à la position dans la liste
                return i + 1; // Les IDs commencent souvent à partir de 1, ajustez si nécessaire
            }
        }
        // Si aucun rayon correspondant n'est trouvé, retourner -1 ou une valeur par défaut
        return -1;
    }

    private void ajouterProduit(String nomProduit, String commentaire, int rayonId) {
        // Créer un objet JSON pour les données à envoyer
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("nom", nomProduit);
            requestBody.put("commentaire", commentaire);
            requestBody.put("createur_id", 1); // Vous pouvez modifier ceci selon vos besoins
            requestBody.put("rayon_id", rayonId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Faire la demande HTTP POST pour ajouter le produit
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=ajouter";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Traitement de la réponse en cas de succès
                        Log.d("ajout_produit_response", response.toString());
                        // Afficher un message à l'utilisateur pour indiquer que le produit a été ajouté
                        Toast.makeText(MainActivity2.this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show();
                        // Réinitialiser les champs de produit et de commentaire après l'ajout
                        ((EditText)findViewById(R.id.nomProduitEditText)).setText("");
                        ((EditText)findViewById(R.id.commentaireEditText)).setText("");
                        // Rafraîchir la liste des produits
                        chargerProduits();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de l'ajout du produit
                        error.printStackTrace();
                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(MainActivity2.this, "Erreur lors de l'ajout du produit", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(postRequest);
    }

    private void supprimerProduit(int idProduit) {
        // Faire la demande HTTP GET pour supprimer le produit avec l'ID spécifié
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=supprimer&idProduit=" + idProduit;
        JsonObjectRequest deleteRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Traitement de la réponse en cas de succès
                        Log.d("suppression_response", response.toString());
                        // Afficher un message à l'utilisateur pour indiquer que le produit a été supprimé
                        Toast.makeText(MainActivity2.this, "Produit supprimé avec succès", Toast.LENGTH_SHORT).show();
                        // Rafraîchir la liste des produits
                        chargerProduits();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de la suppression du produit
                        error.printStackTrace();
                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(MainActivity2.this, "Erreur lors de la suppression du produit", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(deleteRequest);
    }
    private void transfererProduit(int productId) {
        // Créer une requête HTTP GET pour transférer le produit
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=transferer&idProduit=" + productId;
        JsonObjectRequest transferRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Traiter la réponse en cas de succès
                        Log.d("transfer_response", response.toString());
                        // Afficher un message à l'utilisateur pour indiquer que le produit a été transféré
                        Toast.makeText(MainActivity2.this, "Produit transféré", Toast.LENGTH_SHORT).show();
                        // Rafraîchir les listes des produits et du caddie
                        chargerProduits();
                        chargerCaddie();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de la transfert du produit
                        error.printStackTrace();
                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(MainActivity2.this, "Erreur lors de la transfert du produit", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(transferRequest);
    }

    private void trouverIdProduit(String nomProduit) {
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=courses";

        // Faire une demande HTTP pour obtenir la liste des produits
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray produitsArray = response.getJSONArray("result");
                            if (produitsArray != null && produitsArray.length() > 0) {
                                for (int i = 0; i < produitsArray.length(); i++) {
                                    JSONObject produit = produitsArray.getJSONObject(i);
                                    String nom = produit.getString("nom");
                                    if (nom.equals(nomProduit)) {
                                        // Si le nom du produit correspond, récupérer son ID
                                        int productId = produit.getInt("id");
                                        Log.d("Product Found", "Product Name: " + nomProduit + ", ID: " + productId);

                                        // Créer un AlertDialog pour demander à l'utilisateur s'il souhaite transférer ou supprimer le produit
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
                                        builder.setTitle("Confirmation");
                                        builder.setMessage("Etes vous sur ?");

                                        builder.setPositiveButton("Transféré", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Transférer le produit avec l'ID récupéré
                                                transfererProduit(productId);
                                                dialog.dismiss();
                                            }
                                        });

                                        builder.setNegativeButton("Supprimer", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Supprimer le produit avec l'ID récupéré
                                                supprimerProduit(productId);
                                                dialog.dismiss();
                                            }
                                        });

                                        AlertDialog alertDialog = builder.create();
                                        alertDialog.show();

                                        return; // Sortir de la boucle après avoir trouvé le produit
                                    }
                                }
                            }
                            // Gérer le cas où aucun produit avec le nom donné n'est trouvé
                            Log.d("Product Not Found", "Product Name: " + nomProduit + " not found");
                            Toast.makeText(MainActivity2.this, "Produit non trouvé", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity2.this, "Erreur lors de la recherche du produit", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Gérer les erreurs de la requête HTTP
                error.printStackTrace();
                Toast.makeText(MainActivity2.this, "Erreur lors de la recherche du produit", Toast.LENGTH_SHORT).show();
            }
        });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(request);
    }

    private void trouverIdProduitCaddie(final String nomProduit, final int position) {
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=caddie";

        // Faire une demande HTTP pour obtenir la liste des produits dans le caddie
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray produitsArray = response.getJSONArray("result");
                            if (produitsArray != null && produitsArray.length() > 0) {
                                for (int i = 0; i < produitsArray.length(); i++) {
                                    JSONObject produit = produitsArray.getJSONObject(i);
                                    String nom = produit.getString("nom");
                                    if (nom.equals(nomProduit)) {
                                        // Si le nom du produit correspond, récupérer son ID
                                        int productId = produit.getInt("id");
                                        Log.d("Product Found", "Product Name: " + nomProduit + ", ID: " + productId);

                                        // Créer une boîte de dialogue de confirmation de suppression
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
                                        builder.setTitle("Confirmation");
                                        builder.setMessage("Voulez-vous vraiment supprimer ce produit du caddie ?");

                                        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Supprimer le produit avec l'ID récupéré
                                                supprimerProduitCaddie(productId, position);
                                                dialog.dismiss();
                                            }
                                        });

                                        builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });

                                        AlertDialog alertDialog = builder.create();
                                        alertDialog.show();

                                        return; // Sortir de la boucle après avoir trouvé le produit
                                    }
                                }
                            }
                            // Gérer le cas où aucun produit avec le nom donné n'est trouvé
                            Log.d("Product Not Found", "Product Name: " + nomProduit + " not found");
                            Toast.makeText(MainActivity2.this, "Produit non trouvé dans le caddie", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity2.this, "Erreur lors de la recherche du produit dans le caddie", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Gérer les erreurs de la requête HTTP
                error.printStackTrace();
                Toast.makeText(MainActivity2.this, "Erreur lors de la recherche du produit dans le caddie", Toast.LENGTH_SHORT).show();
            }
        });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(request);
    }

    private void supprimerProduitCaddie(int productId, final int position) {
        // Faire la demande HTTP GET pour supprimer le produit du caddie avec l'ID spécifié
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=supprimer&idProduit=" + productId;
        JsonObjectRequest deleteRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Traitement de la réponse en cas de succès
                        Log.d("suppression_caddie_response", response.toString());
                        // Afficher un message à l'utilisateur pour indiquer que le produit a été supprimé du caddie
                        Toast.makeText(MainActivity2.this, "Produit supprimé du caddie avec succès", Toast.LENGTH_SHORT).show();

                        // Rafraîchir la liste du caddie après la suppression
                        chargerCaddie();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de la suppression du produit du caddie
                        error.printStackTrace();
                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(MainActivity2.this, "Erreur lors de la suppression du produit du caddie", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(deleteRequest);
    }

    private void resetterProduits() {
        // Faire la demande HTTP GET pour supprimer tous les produits
        String url = "http://sio.jbdelasalle.com/~gpetit/courses/index.php?action=supprimer_tous";
        JsonObjectRequest deleteRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Traitement de la réponse en cas de succès
                        Log.d("reset_response", response.toString());
                        // Afficher un message à l'utilisateur pour indiquer que tous les produits ont été supprimés
                        Toast.makeText(MainActivity2.this, "Tous les produits ont été supprimés", Toast.LENGTH_SHORT).show();
                        // Rafraîchir la liste des produits
                        chargerProduits();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de la suppression de tous les produits
                        error.printStackTrace();
                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(MainActivity2.this, "Erreur lors de la suppression de tous les produits", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ajouter la demande à la file de requêtes
        requestQueue.add(deleteRequest);
    }

    private String getRayonName(int rayonId) {
        // Récupérer la liste des rayons du Spinner
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) rayonsSpinner.getAdapter();
        // Parcourir la liste des rayons pour trouver celui qui correspond à l'ID donné
        for (int i = 0; i < adapter.getCount(); i++) {
            if (i + 1 == rayonId) {
                // Récupérer le nom du rayon correspondant à l'ID
                return adapter.getItem(i);
            }
        }
        // Si aucun rayon correspondant n'est trouvé, retourner une chaîne vide ou une valeur par défaut
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the repeated task when the activity is destroyed
        handler.removeCallbacksAndMessages(null);
    }
}