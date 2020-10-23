package com.mrguide.mrguide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.mrguide.mrguide.Pomodoro.MainActivityPomodoro;
import com.mrguide.mrguide.ToDo.MyDatabase;
import com.mrguide.mrguide.ToDo.RecyclerViewAdapter;
import com.mrguide.mrguide.ToDo.Todo;
import com.mrguide.mrguide.ToDo.TodoNoteActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements RecyclerViewAdapter.ClickListener, AdapterView.OnItemSelectedListener {
    //Navigation
    private DrawerLayout dl;
    private ActionBarDrawerToggle abdt;


    //Dark Mode
    Switch darkMode;
    MenuItem menuItem;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;
    Boolean isNightModeOn;


    //TODO
    MyDatabase myDatabase;
    RecyclerView recyclerView;
    Spinner spinner;
    RecyclerViewAdapter recyclerViewAdapter;
    FloatingActionButton floatingActionButton;
    private String[] categories = {
            "All",
            "Personal",
            "Work",
            "Study",
            "Others"
    };

    ArrayList<Todo> todoArrayList = new ArrayList<>();
    ArrayList<String> spinnerList = new ArrayList<>(Arrays.asList(categories));

    public static final int NEW_TODO_REQUEST_CODE = 200;
    public static final int UPDATE_TODO_REQUEST_CODE = 300;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Navigation
        dl = (DrawerLayout)findViewById(R.id.dl);
        abdt = new ActionBarDrawerToggle(this,dl,R.string.Open,R.string.Close);
        abdt.setDrawerIndicatorEnabled(true);
        dl.addDrawerListener(abdt);
        abdt.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView nav_view =(NavigationView)findViewById(R.id.nav_view);

        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                int id = menuItem.getItemId();

                if(id==R.id.todo){
                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                if(id==R.id.timer){
                    Intent intent = new Intent(getApplicationContext(), MainActivityPomodoro.class);
                    startActivity(intent);
                    finish();
                }
//                if(id==R.id.money_manager){
//                    Toast.makeText(MainActivity.this,"Money Manager",Toast.LENGTH_LONG).show();
//                }
//                if(id==R.id.dark_menu){
//                    Toast.makeText(MainActivity.this,"Dark Mode",Toast.LENGTH_LONG).show();
//                }


                return true;
            }
        });



        //Dark Mode
        menuItem = nav_view.getMenu().findItem(R.id.dark_menu);
        darkMode = (Switch) menuItem.getActionView().findViewById(R.id.dark_mode_switch);
        sharedPreferences = getSharedPreferences("AppSettingPrefs",0);
        sharedPreferencesEditor = sharedPreferences.edit();
        isNightModeOn = sharedPreferences.getBoolean("NightMode",false);

        if(isNightModeOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkMode.setChecked(true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            darkMode.setChecked(false);
        }

        darkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    sharedPreferencesEditor.putBoolean("NightMode",true);
                    sharedPreferencesEditor.apply();
                }else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    sharedPreferencesEditor.putBoolean("NightMode",false);
                    sharedPreferencesEditor.apply();
                }
            }
        });



        //TODO
        initViews();
        myDatabase = Room.databaseBuilder(getApplicationContext(), MyDatabase.class, MyDatabase.DB_NAME).fallbackToDestructiveMigration().build();
        checkIfAppLaunchedFirstTime();

        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(0);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TodoNoteActivity.class);
                startActivityForResult(intent, NEW_TODO_REQUEST_CODE);
            }
        });


    }



    //Navigation
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return abdt.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    //TODO
    private void initViews() {
        floatingActionButton = findViewById(R.id.fab);
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);

    }

    @Override
    public void launchIntent(int id) {
        Intent intent = new Intent(getApplicationContext(), TodoNoteActivity.class);
        intent.putExtra("id", id);
        startActivityForResult(intent, UPDATE_TODO_REQUEST_CODE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


        if (position == 0) {
            loadAllTodos();
        } else {
            String string = parent.getItemAtPosition(position).toString();
            loadFilteredTodos(string);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @SuppressLint("StaticFieldLeak")
    private void loadFilteredTodos(String category) {
        new AsyncTask<String, Void, List<Todo>>() {
            @Override
            protected List<Todo> doInBackground(String... params) {
                return myDatabase.daoAccess().fetchTodoListByCategory(params[0]);

            }

            @Override
            protected void onPostExecute(List<Todo> todoList) {
                recyclerViewAdapter.updateTodoList(todoList);
            }
        }.execute(category);

    }


    @SuppressLint("StaticFieldLeak")
    private void fetchTodoByIdAndInsert(int id) {
        new AsyncTask<Integer, Void, Todo>() {
            @Override
            protected Todo doInBackground(Integer... params) {
                return myDatabase.daoAccess().fetchTodoListById(params[0]);

            }

            @Override
            protected void onPostExecute(Todo todoList) {
                recyclerViewAdapter.addRow(todoList);
            }
        }.execute(id);

    }

    @SuppressLint("StaticFieldLeak")
    private void loadAllTodos() {
        new AsyncTask<String, Void, List<Todo>>() {
            @Override
            protected List<Todo> doInBackground(String... params) {
                return myDatabase.daoAccess().fetchAllTodos();
            }

            @Override
            protected void onPostExecute(List<Todo> todoList) {
                recyclerViewAdapter.updateTodoList(todoList);
            }
        }.execute();
    }

    private void buildDummyTodos() {
        Todo todo = new Todo();
        todo.name = "Android Retrofit Tutorial";
        todo.description = "Cover a tutorial on the Retrofit networking library using a RecyclerView to show the data.";
        todo.category = "Personal";

        todoArrayList.add(todo);

        todo = new Todo();
        todo.name = "iOS TableView Tutorial";
        todo.description = "Covers the basics of TableViews in iOS using delegates.";
        todo.category = "Work";

        todoArrayList.add(todo);

        todo = new Todo();
        todo.name = "Kotlin Arrays";
        todo.description = "Cover the concepts of Arrays in Kotlin and how they differ from the Java ones.";
        todo.category = "Study";

        todoArrayList.add(todo);

        todo = new Todo();
        todo.name = "Swift Arrays";
        todo.description = "Cover the concepts of Arrays in Swift and how they differ from the Java and Kotlin ones.";
        todo.category = "Others";

        todoArrayList.add(todo);
        insertList(todoArrayList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //reset spinners
            spinner.setSelection(0);

            if (requestCode == NEW_TODO_REQUEST_CODE) {
                long id = data.getLongExtra("id", -1);
                Toast.makeText(getApplicationContext(), "Task Added", Toast.LENGTH_SHORT).show();
                fetchTodoByIdAndInsert((int) id);

            } else if (requestCode == UPDATE_TODO_REQUEST_CODE) {

                boolean isDeleted = data.getBooleanExtra("isDeleted", false);
                int number = data.getIntExtra("number", -1);
                if (isDeleted) {
                    Toast.makeText(getApplicationContext(), number + " Task Completed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), number + " Task Updated", Toast.LENGTH_SHORT).show();
                }

                loadAllTodos();

            }


        } else {
            //Toast.makeText(getApplicationContext(), "No action done by user", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void insertList(List<Todo> todoList) {
        new AsyncTask<List<Todo>, Void, Void>() {
            @Override
            protected Void doInBackground(List<Todo>... params) {
                myDatabase.daoAccess().insertTodoList(params[0]);

                return null;

            }

            @Override
            protected void onPostExecute(Void voids) {
                super.onPostExecute(voids);
            }
        }.execute(todoList);

    }

    private void checkIfAppLaunchedFirstTime() {
        final String PREFS_NAME = "SharedPrefs";

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings.getBoolean("firstTime", true)) {
            settings.edit().putBoolean("firstTime", false).apply();
            buildDummyTodos();
        }
    }
}