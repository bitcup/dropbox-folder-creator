package com.bitcup.dropboxfoldercreator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.bitcup.dropboxfoldercreator.actor.SupervisorActor;
import com.bitcup.dropboxfoldercreator.config.Config;
import com.bitcup.dropboxfoldercreator.domain.Student;
import com.bitcup.dropboxfoldercreator.dropbox.DropboxClient;
import com.bitcup.dropboxfoldercreator.message.CreateFolders;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        App app = new App();
        List<Student> students = app.getStudents(args[0]);
        LOGGER.info("There are {} students to process", students.size());

        DropboxClient dropboxClient = DropboxClient.getInstance();
        dropboxClient.createFolderIfNotExists(Config.val("dropbox.top.folder"), "");

        final ActorSystem system = ActorSystem.create("dropbox-uploader-app");
        final ActorRef supervisorActorRef = system.actorOf(Props.create(SupervisorActor.class, students), "supervisor");

        // create folders
        supervisorActorRef.tell(new CreateFolders(), supervisorActorRef);

        system.awaitTermination();
        LOGGER.info("Shutting down actor system...");
    }

    private List<Student> getStudents(String arg) throws IOException {
        return FileUtils.readLines(new File(arg)).stream().map(Student::new).collect(Collectors.toList());
    }

}
