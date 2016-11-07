package com.bitcup.dropboxfoldercreator.actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.bitcup.dropboxfoldercreator.config.Config;
import com.bitcup.dropboxfoldercreator.domain.Student;
import com.bitcup.dropboxfoldercreator.dropbox.DropboxClient;
import com.bitcup.dropboxfoldercreator.message.CreateFolders;
import com.bitcup.dropboxfoldercreator.message.CreationResult;
import com.bitcup.dropboxfoldercreator.message.RecordRetry;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.sharing.AccessLevel;
import scala.Option;

import java.util.Random;

/**
 * Creates folders for a single student.
 *
 * @author bitcup
 */
public class FolderCreatorActor extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private DropboxClient dropboxClient = DropboxClient.getInstance();
    private Student student;
    private ActorRef supervisor;

    private Random random = new Random();

    public FolderCreatorActor(Student student, ActorRef supervisor) {
        this.student = student;
        this.supervisor = supervisor;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateFolders) {
            createFolders();
        } else {
            unhandled(message);
            log.info("unhandled message {}", message);
        }
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        log.debug("pre-restart - reason: {}, message: {}", reason.getMessage(), message.getClass());
        // tell supervisor that we're retrying this student
        supervisor.tell(new RecordRetry(student), getSelf());
        // re-send the same message to ourselves in order to retry creating the folders
        getSelf().tell(message.get(), supervisor);
        super.preRestart(reason, message);
    }

    private void createFolders() throws DbxException {
        try {
            int r = random.nextInt(3);
            if (r == 0) {
                throw new DbxException("intermittent dbx failure: could not create folders for " + student.getStudentFolderName());
            }
            if (r == 1) {
                throw new IllegalArgumentException("irrecoverable failure: could not create folders for " + student.getStudentFolderName());
            }
            if (Config.val("invoke.dbx").equals("true")) {
                FolderMetadata studentFolder = dropboxClient.createFolderIfNotExists(student.getStudentFolderName(), Config.val("dropbox.top.folder"));
                dropboxClient.shareFolderAndGetUrl(studentFolder, student.getEmail(), AccessLevel.EDITOR);
            }
        } catch (IllegalArgumentException e) {
            // we tell supervisor about failure
            supervisor.tell(new CreationResult.Failure(student), getSelf());
            throw e;
        }
        log.info("success: folders created for: {}", student.getStudentFolderName());
        supervisor.tell(new CreationResult.Success(student, "link goes here"), getSelf());
    }
}
