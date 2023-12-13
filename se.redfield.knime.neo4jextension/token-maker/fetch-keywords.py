import requests
from bs4 import BeautifulSoup

KEYWORDS_URL = 'https://neo4j.com/docs/cypher-manual/current/syntax/reserved/'
TYPES_URL = 'https://neo4j.com/docs/cypher-manual/current/values-and-types/property-structural-constructed/'
FUNCTIONS_URL = 'https://neo4j.com/docs/cypher-manual/current/functions/'
OPERATORS_URL = 'https://neo4j.com/docs/cypher-manual/current/syntax/operators/'

def query(url):
    html = requests.get(url).text
    return BeautifulSoup(html, features='lxml')
def save(words, filename):
    with open(filename, 'w') as file:
        file.write('\n'.join(words))

if __name__ == '__main__':
    bs = query(KEYWORDS_URL)
    words = [t.text for t in bs.select('li code')]
    save(words, 'keywords.txt')

    bs = query(TYPES_URL)
    words = [t.text for t in bs.select('table.synonyms code')]
    save(words, 'types.txt')

    bs = query(FUNCTIONS_URL)
    words = [t.text.replace('()', '') for t in bs.select('table a code')]
    save(words, 'functions.txt')
