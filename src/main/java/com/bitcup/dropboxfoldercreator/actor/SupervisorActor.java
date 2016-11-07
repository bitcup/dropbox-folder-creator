package com.bitcup.dropboxfoldercreator.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.bitcup.dropboxfoldercreator.domain.Student;
import com.bitcup.dropboxfoldercreator.message.CreateFolders;
import com.bitcup.dropboxfoldercreator.message.CreationResult;
import com.bitcup.dropboxfoldercreator.message.RecordRetry;
import com.dropbox.core.DbxException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supervises {@link FolderCreatorActor} instances.  Keeps track of retries (by student) whenever
 * the dropbox folder creation fails due to a recoverable error (e.g., rate limit)
 *
 * @author bitcup
 */
public class SupervisorActor extends UntypedActor {

    final private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    // students to be processed
    final private List<Student> students;
    // number of dropbox folder creation retries by student (for reporting)
    final private Map<Student, AtomicInteger> retries = Maps.newConcurrentMap();
    // completed students (success or failure) - used to stop the program by terminating actor system
    final private AtomicInteger completed = new AtomicInteger(0);
    final private Set<Student> failures = Sets.newConcurrentHashSet();

    public SupervisorActor(List<Student> students) {
        this.students = Lists.newArrayList();
        students.stream().forEach(this.students::add);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateFolders) {
            for (Student student : students) {
                final String actorName = student.getStudentFolderName().substring(1);
                // pass supervisor ref to child actor so it can use it to re-send itself the same message on restarts
                ActorRef folderCreatorActor = context().actorOf(Props.create(FolderCreatorActor.class, student, getSelf()), actorName);
                folderCreatorActor.tell(new CreateFolders(), getSelf());
            }
        } else if (message instanceof RecordRetry) {
            Student student = ((RecordRetry) message).student;
            if (!retries.containsKey(student)) {
                retries.put(student, new AtomicInteger(0));
            }
            retries.get(student).incrementAndGet();
        } else if (message instanceof CreationResult.Success) {
            completed.incrementAndGet();
            checkTermination();
        } else if (message instanceof CreationResult.Failure) {
            failures.add(((CreationResult.Failure) message).student);
            completed.incrementAndGet();
            checkTermination();
        } else {
            unhandled(message);
            log.info("unhandled message {}", message);
        }
    }

    private void checkTermination() throws InterruptedException {
        if (completed.get() == students.size()) {
            log.info("--------- SUCCESS ------------");
            students.stream().filter(student -> !failures.contains(student)).forEach(s -> log.info("{}: {} retries", s.getStudentFolderName(), retries.containsKey(s) ? retries.get(s) : 0));
            log.info("--------- FAILURE ------------");
            failures.stream().forEach(s -> log.info("{}: {} retries", s.getStudentFolderName(), retries.containsKey(s) ? retries.get(s) : 0));
            getContext().system().terminate();
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"), t -> {
            if (t instanceof DbxException) {
                // note: restart calls stop, so we cannot put code in supervised actor's postStop
                // that is specific to IllegalArgumentException because it will be executed also
                // when DbxException happens and the supervised actor is restarted
                return SupervisorStrategy.restart();
            } else if (t instanceof IllegalArgumentException) {
                return SupervisorStrategy.stop();
            } else {
                return SupervisorStrategy.escalate();
            }
        });
    }

}
