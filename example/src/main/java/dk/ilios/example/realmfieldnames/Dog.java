package dk.ilios.example.realmfieldnames;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;

public class Dog extends RealmObject {
    @LinkingObjects("favoriteDog")
    public final RealmResults<Person> people = null;
    public String name;
    public int age;
    public BestFriend bestFriend;
}
