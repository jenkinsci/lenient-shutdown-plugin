style(type: "text/css", '''
    #lenient-shutdown-msg {
        font-weight: bold;
        font-size: larger;
        color: white;
        background-color: #ef2929;
        text-align: center;
        padding: 0.5em;
    }
''')

if(it.goingToShutdown) {
    div(id: "lenient-shutdown-msg", it.shutdownMessage)
}
