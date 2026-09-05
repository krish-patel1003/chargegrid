export function Feedback({
    loading,
    error,
    retry,
}: {
    loading: boolean;
    error: string;
    retry?: () => void;
}) {
    if (loading) {
        return <p className="muted">Loading...</p>;
    }
    if (!error) {
        return null;
    }
    return (
        <div className="card info">
            <p className="muted">{error}</p>
            {retry && (
                <button className="outline" onClick={retry}>
                    Try again
                </button>
            )}
        </div>
    );
}
