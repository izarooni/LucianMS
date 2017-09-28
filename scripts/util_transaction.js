// Create a log of a player's in-game shop transaction
// of what they purchased, what it cost and when
function createTransaction(playerId, log) {
    try {
        var ps = Packages.tools.DatabaseConnection.getConnection().prepareStatement("insert into transactions values (default, ?, ?, default)", Packages.java.sql.Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, playerId);
        ps.setString(2, log);
        ps.executeUpdate();
        var rs = ps.getGeneratedKeys();
        try {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            rs.close();
            ps.close();
        }
    } catch (e) {
        print(e);
        return -1; // god i hope this never happens
    }
}