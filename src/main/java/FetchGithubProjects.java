import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.StreamSupport;

public class FetchGithubProjects {

    private static final String CODECOV = "https://codecov.io/gh/";
    private static final String COVERALLS = "https://coveralls.io/";
    private static final String CODECLIMATE = "https://codeclimate.com/github/";


    public static boolean hasTravisCI(GHRepository repo) {
        try {
            repo.getFileContent(".travis.yml");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean hasCodeCoverageService(GHRepository repo) {
        try {
            var content = repo.getFileContent("README.md");
            var reader = new BufferedReader(new InputStreamReader(content.read()));
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.contains(CODECOV) || line.contains(COVERALLS) || line.contains(CODECLIMATE))
                    return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static void printIterableInfo(final PagedSearchIterable<GHRepository> iterable) {
        int totalNumberOfFoundRepositories = iterable.getTotalCount();
        System.out.printf("%s repositories has been found\n", totalNumberOfFoundRepositories);
        if (iterable.isIncomplete()) {
            System.out.println("Results are incomplete, there might be others repositories");
        } else {
            System.out.println("These are all the repositories that match the requirements");
        }

        if (totalNumberOfFoundRepositories > 1000) {
            System.out.printf(
                    "Only first %s repositories out of %s can be retrieved!" +
                            " Repositories will be sorted by stars in descending order.\n\n",
                    1000,
                    totalNumberOfFoundRepositories
            );
        }
    }


    public static void main(String[] args) throws IOException {
        var github = GitHub.connect();

        System.out.println(github.getRateLimit());


        var repos = github
                .searchRepositories()
                .language("java")
                .stars("25..29")
                .sort(GHRepositorySearchBuilder.Sort.STARS)
                .order(GHDirection.DESC)
                .list()
                .withPageSize(100);


        printIterableInfo(repos);


        StreamSupport
                .stream(repos.spliterator(), true)
                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));


//        var path = Paths.get("/Users/jhejderup/crap2.txt");


//        Files.readAllLines(path)
//                .stream()
//                .map(l -> l.split(","))
//                .map(ls ->String.format("https://github.com/%s/compare/%s..%s", ls[0],ls[1],ls[3]))
//                .forEach(System.out::println);


//        Files.readAllLines(path)
//                .stream()
//                .map(l -> l.split(","))
//                .map(ls -> {
//                    try {
//                        var nonbuildCount = Arrays.stream(github
//                                .getRepository(ls[0])
//                                .getCompare(ls[1],ls[3])
//                                .getFiles())
//                                .filter(k -> !k.getFileName().contains("pom.xml") && !k.getFileName().contains("build.gradle"))
//                                .count();
//
//                        if (nonbuildCount > 0){
//                            return null;
//                        } else {
//                            return ls;
//                        }
//                    } catch (IOException e) {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .map(ls -> String.join(",", ls))
//                .forEach(System.out::println);


//        var files = github.getRepository("gwtbootstrap3/gwtbootstrap3")
//                .getCompare("a04344eda3f16bcd70d822b03c2dfdf4e639848e","a96eb983b06ca5e271b5393b8710c46c6f105dc0")
//                .getFiles();


//        repos.forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));


//        repos.withPageSize(20000).
//                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));

//        repos.asList()
//                .stream()
//                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));


//        StreamSupport
//                .stream(repos.spliterator(), true)
//                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));
//
//
//        repositories
//                .filter(repo -> Objects.equals(repo.getLanguage(), "Java"))
//                .filter(FetchGithubProjects::hasTravisCI)
//                .filter(FetchGithubProjects::hasCodeCoverageService)
//                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));
//
//
////        repositories
////                .filter(repo -> Objects.equals(repo.getLanguage(), "Java"))
////                .filter(FetchGithubProjects::hasTravisCI)
////                .forEach(repo -> {
////
////
////                    try {
////                        var f = new File(String.format("/Users/jhejderup/Studies/uppdatera/%s", repo.getFullName()));
////                        Git.cloneRepository().setURI(String.format("%s.git", repo.getHtmlUrl().toString()))
////                                .setDirectory(f)
////                                .call();
////                        System.out.println(String.format("%s.git", repo.getHtmlUrl().toString()));
////                    } catch (GitAPIException e) {
////                        e.printStackTrace();
////                    }
////                });


    }
}
