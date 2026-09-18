# Navires fantômes

## Configuration serveur

Tous les réglages propres aux fantômes sont dans `config/smallships-ghosts.json` :
`general` pour l'apparition et la détection, `ships` pour les profils par modèle.
Les paramètres généraux partagés par tous les navires (par exemple la vitesse
de base des modèles et les dégâts globaux des canons) restent dans le TOML.

Le JSON est créé et chargé au démarrage du serveur (ou du monde solo).
Pour appliquer une modification, redémarrer le serveur / rouvrir le monde.
Il n'y a pas de rechargement à chaud par `/reload`.

Au premier démarrage de cette version, la section `general` manquante est ajoutée
au JSON depuis les anciennes valeurs TOML. Les profils, récompenses et autres champs
déjà présents dans le JSON sont conservés. Une section `general` existante est prioritaire.
Une copie de l'ancien TOML est conservée dans
`config/smallships-common.toml.before-ghost-migration.bak` avant le nettoyage
de ses anciennes clés. Si le JSON est complété, une sauvegarde
`smallships-ghosts.before-migration-....json.bak` est également créée.

Si le JSON n'existait pas, les anciens réglages de loot sont repris pour chaque
modèle, **y compris une liste vide**. Les anciennes clés `pirateShips...` et
`pirateLoot...` ne sont plus déclarées ni utilisées comme configuration active.
L'ancien `pirateShipsDespawnDistance`, qui n'avait plus d'effet, est supprimé :
les délais de départ et de naufrage restent dans chaque profil.

Un JSON invalide est conservé et signalé dans le journal. Les nouvelles
apparitions sont désactivées jusqu'à correction et redémarrage.

| Clé de general | Effet | Défaut |
| --- | --- | --- |
| enabled | Active les apparitions naturelles | true |
| spawnIntervalTicks | Intervalle entre les vérifications, en ticks (20 ticks = 1 seconde) | 1200 |
| spawnChance | Chance par joueur admissible et vérification, entre 0 et 1 | 0.35 |
| maxNearby | Maximum de fantômes dans la zone de comptage à 192 blocs du joueur (épaves comprises) | 2 |
| requiredCannonBalls | Boulets chargés requis sur le bateau du joueur | 5 |
| detectionRange | Distance de détection des joueurs en blocs | 72 |
| spawnDistanceMin / spawnDistanceMax | Distance d'apparition autour du bateau du joueur, en blocs | 32 / 48 |
| cannonInaccuracy | Dispersion des boulets : augmenter pour rendre les tirs moins précis | 8 |

Pour les tests : `spawnIntervalTicks: 100` et `spawnChance: 1.0` donnent
une vérification toutes les cinq secondes avec une tentative certaine.
Les conditions d'eau, d'armement, d'espace et de limite locale restent applicables.

Voir [l'exemple complet](smallships-ghosts.example.json), à copier dans le fichier
de configuration du serveur si souhaité (il contient un butin non vide).
Cet exemple illustre des réglages personnalisés ; ce ne sont pas tous les défauts.

## Réglages par navire

Les clés `cog`, `galley` et `brigg` sont obligatoires sous `ships`.
Chaque profil possède :

| Clé | Effet | Défaut |
| --- | --- | --- |
| health | Vie maximale du navire à son apparition | Cog 250, Galley 300, Brigg 400 |
| despawnSeconds | Disparition de l'épave après le début du naufrage, en secondes | 30 |
| combatIdleSeconds | Délai sans cible admissible avant la fuite par plongée | 20 |
| captainHealth | Vie du capitaine | 40 |
| crewHealth | Vie de chaque autre marin | 20 |
| crewMin / crewMax | Nombre aléatoire de marins, **en plus** du capitaine | 1 / 3 |
| lootPools | Groupes de tirages du butin | Migration des anciens réglages |

Les temps sont des secondes de simulation : ils avancent uniquement quand le
navire est chargé et tické. Le temps déjà écoulé d'une épave est sauvegardé.
La plongée de fuite dure trois secondes supplémentaires et ne donne pas de butin.
Le délai des navires normaux reste indépendant, dans le TOML.

Le capitaine occupe la première place ; le nombre de marins est plafonné aux
places restantes du modèle. Les PNJ utilisent le modèle de joueur avec le skin
fourni dans `textures/entity/ghost_crew.png`, lumineux et translucide. Ils restent assis, sans IA de combat
individuelle. Seule la mort du **capitaine** met la vie du bateau à zéro et le fait
couler ; tuer un marin ne le coule pas. Leur propre table de butin est vide.

Les vies et l'équipage sont définis au spawn : tester avec de nouveaux navires.
Les récompenses et les délais utilisent le profil chargé, y compris pour les
anciens navires qui n'ont pas encore donné leur butin.

## Tables de butin et multi-loot

Chaque `lootPools` est évalué indépendamment ; plusieurs groupes peuvent donc
donner des récompenses au même naufrage.

1. `chance` du groupe : probabilité de l'activer, de 0 à 1 (`0.25` = 25 %).
2. `rollsMin` / `rollsMax` : nombre de tirages, bornes incluses.
3. Chaque tirage sélectionne une entrée selon son `weight` relatif.
4. La `chance` de l'entrée sélectionnée est ensuite testée. En cas d'échec,
   ce tirage ne donne rien : il n'est pas relancé.
5. Tous les objets de `items` de cette entrée sont donnés ensemble, avec leur
   quantité aléatoire inclusive `min` / `max`.

`uniqueEntries: true` empêche de sélectionner deux fois la même entrée dans
ce groupe (même si sa chance échoue). Les tirages s'arrêtent quand il n'y a plus
d'entrées. Avec `false`, une entrée peut être choisie plusieurs fois.
Les poids ne doivent pas totaliser 100.

Exemple : deux entrées de poids 70 et 30 avec `chance: 1` ont 70 % et 30 %
de chances par tirage. Si la seconde a `chance: 0.5`, elle donne effectivement
son lot dans 15 % des tirages, et les 15 % restants sont vides.

Pour des objets ayant chacun une chance **indépendante**, utiliser un groupe
par objet, avec un tirage et une seule entrée. Pour donner plusieurs objets
ensemble, les mettre dans le même `items`.
Le nombre de tirages n'est pas le nombre d'unités : un tirage peut donner plusieurs
types d'objets, chacun en plusieurs exemplaires.

Les identifiants d'autres mods sont acceptés si les objets sont enregistrés.
Un identifiant inconnu est ignoré et signalé dans le journal.
Les grandes quantités sont divisées en piles valides.
`lootPools: []`, un groupe vide ou zéro tirage désactivent le butin concerné.

## Naufrage et diagnostic des drops

Les récompenses sont générées **au naufrage**, une seule fois, et déposées
près de la surface à la verticale du navire, pas stockées au fond de l'épave.
Cela vaut pour une destruction de la coque comme pour la mort du capitaine.
Le drapeau de récompense est sauvegardé pour éviter de donner à nouveau le butin
après rechargement. Une fuite ne génère aucun objet.

Si rien n'apparaît, vérifier :
- `/gamerule doEntityDrops` doit être `true`.
- Le profil du bon type doit avoir des entrées non vides.
- Les probabilités et tirages peuvent réellement donner un résultat vide.
- Redémarrer après modification du JSON.
- Chercher `Ghost ship ... spawned N reward stacks` dans `logs/latest.log`,
  ou une erreur de configuration / un identifiant inconnu.

Pour un test garanti, définir un seul groupe avec `chance: 1`,
`rollsMin: 1`, `rollsMax: 1`, une entrée de poids 1 et chance 1,
et un diamant avec `min: 1`, `max: 1`.

## Apparition et animation

Le joueur doit être à bord d'un navire non coulé, non IA, avec au moins un canon
installé et au moins `general.requiredCannonBalls` boulets chargés (5 par défaut).
Les objets simplement présents dans un inventaire ne comptent pas.
Cette condition concerne les nouvelles apparitions, pas la poursuite d'un combat.

Le modèle est tiré indépendamment du navire du joueur :
Cog 55 %, Galley 30 %, Brigg 15 %.
Les contraintes d'eau profonde, espace libre et limite de navires proches restent
applicables. Le Galleon n'est pas sélectionné par le spawn naturel.

L'apparition a lieu à 32–48 blocs par défaut, avec une distance minimale vérifiée
aux autres bateaux proches. Les noms des fantômes sont masqués.
L'émergence dure quatre secondes :
la proue se lève jusqu'à 28 degrés, la coque retombe avec une éclaboussure,
puis un petit rebond amorti la ramène à plat.

## Vérification de développement

### Navigation de combat

Le navire conserve une manœuvre pendant quelques secondes : bordée lente,
cercle rapproché, croisement devant la cible, contournement par l'arrière,
zigzag ou dégagement. La poursuite vise une position anticipée.
Après six secondes sans angle de tir, il change de tactique et de côté.
Un gros impact peut provoquer une esquive ou un dégagement si la coque est
très endommagée. Une immobilité de quatre secondes déclenche une tentative
de sortie de blocage. L'évitement compare plusieurs caps et conserve brièvement
celui choisi pour limiter les changements de direction incessants.
Les vitesses restent plafonnées par le modèle ; la précision et les règles
de dégâts des canons ne changent pas. La mémoire tactique est réinitialisée
au rechargement de l'entité ou au changement de cible.

Les décisions sont couvertes par `common:ghostCombatTest` (distances,
blocage, réaction aux impacts, diversité, valeurs bornées).
Les trajectoires réelles, collisions et l'équilibrage restent à valider en jeu.

`gradlew build` exécute aussi `common:ghostLootTest` : tirages pondérés,
chances nulles/certaines, lots multiples, entrées uniques, groupes vides,
bornes des tirages, groupes indépendants et rejet d'une vie non finie.
La présence effective des objets et le rendu de l'équipage doivent aussi être
testés en jeu ; ces tests ne simulent pas un serveur Minecraft.
