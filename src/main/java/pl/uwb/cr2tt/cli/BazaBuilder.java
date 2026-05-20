package pl.uwb.cr2tt.cli;

public class BazaBuilder {

    public static void main(String[] args) {
        String dbPath = "C:\\Users\\magda\\Desktop\\studia\\Praca magisterska\\program\\data\\baza_tdb2";

        String ttlPath = "C:\\Users\\magda\\Desktop\\studia\\Praca magisterska\\program\\data\\turtle\\uniprotkb_reviewed_eukaryota_opisthokonta_metazoa_33208_0.ttl";

        tdb2.tdbloader.main(new String[]{"--loc=" + dbPath, ttlPath});

    }
}