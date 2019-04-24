import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FetchGithubProjects {



    public static void main(String[] args) throws IOException {
        var github = GitHub.connect();

        System.out.println(github.getRateLimit());

        var result = github.searchContent()
                .q("gradle/dependency-locks")
                .in("path").list();


        var repositories = StreamSupport
                .stream(result.spliterator(), false)
                .map(r -> r.getOwner())
                .distinct()
                .collect(Collectors.toList());

        repositories.stream()
                .map(repo -> {
                    try {
                        return github.getRepository(repo.getFullName());
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(repo -> Objects.equals(repo.getLanguage(), "Java"))
                .map(repo -> repo.getHtmlUrl())
                .forEach(System.out::println);








    }
}
